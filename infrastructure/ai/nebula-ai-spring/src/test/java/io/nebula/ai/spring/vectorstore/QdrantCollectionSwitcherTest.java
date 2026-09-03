package io.nebula.ai.spring.vectorstore;

import com.google.common.util.concurrent.Futures;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.AliasDescription;
import io.qdrant.client.grpc.Collections.AliasOperations;
import io.qdrant.client.grpc.Collections.CollectionOperationResponse;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Qdrant 集合切换器（R3 §4.2、§8）—— mock {@link QdrantClient}，不要求真实 Qdrant
 * <p>
 * 断言 {@code updateAliasesAsync} 收到的操作列表（别名存在 → Delete+Create；不存在 → 仅 Create）；
 * {@code drop} 在 {@code listCollectionAliasesAsync} 非空时拒绝；维度为 0 时报配置错误；
 * {@code prepare} 缺集合时按维度建集合；{@code ExecutionException} 拆包为含集合名的明确异常。
 */
class QdrantCollectionSwitcherTest {

    private QdrantClient client;
    private QdrantCollectionSwitcher switcher;

    @BeforeEach
    void setUp() {
        client = mock(QdrantClient.class);
        switcher = new QdrantCollectionSwitcher(client, "vec", 768, "cosine", 5);
    }

    @Test
    void switchTo_existingAlias_submitsDeleteThenCreate() {
        when(client.listAliasesAsync()).thenReturn(Futures.immediateFuture(List.of(
                AliasDescription.newBuilder().setAliasName("vec").setCollectionName("vec-g1").build())));
        when(client.updateAliasesAsync(anyList()))
                .thenReturn(Futures.immediateFuture(CollectionOperationResponse.newBuilder().build()));

        switcher.switchTo("vec", "vec-g2");

        @SuppressWarnings("unchecked")
        var captor = forClass(List.class);
        verify(client).updateAliasesAsync(captor.capture());
        @SuppressWarnings("unchecked")
        List<AliasOperations> ops = captor.getValue();
        assertThat(ops).hasSize(2);
        assertThat(ops.get(0).hasDeleteAlias()).isTrue();
        assertThat(ops.get(0).getDeleteAlias().getAliasName()).isEqualTo("vec");
        assertThat(ops.get(1).hasCreateAlias()).isTrue();
        assertThat(ops.get(1).getCreateAlias().getCollectionName()).isEqualTo("vec-g2");
        assertThat(ops.get(1).getCreateAlias().getAliasName()).isEqualTo("vec");
    }

    @Test
    void switchTo_missingAlias_onlyCreates() {
        when(client.listAliasesAsync()).thenReturn(Futures.immediateFuture(List.of()));
        when(client.updateAliasesAsync(anyList()))
                .thenReturn(Futures.immediateFuture(CollectionOperationResponse.newBuilder().build()));

        switcher.switchTo("vec", "vec-g1");

        @SuppressWarnings("unchecked")
        var captor = forClass(List.class);
        verify(client).updateAliasesAsync(captor.capture());
        @SuppressWarnings("unchecked")
        List<AliasOperations> ops = captor.getValue();
        assertThat(ops).hasSize(1);
        assertThat(ops.get(0).hasCreateAlias()).isTrue();
        assertThat(ops.get(0).getCreateAlias().getCollectionName()).isEqualTo("vec-g1");
    }

    @Test
    void resolveCurrent_returnsCollectionForAlias_nullWhenAbsent() {
        when(client.listAliasesAsync()).thenReturn(Futures.immediateFuture(List.of(
                AliasDescription.newBuilder().setAliasName("vec").setCollectionName("vec-g2").build())));
        assertThat(switcher.resolveCurrent("vec")).isEqualTo("vec-g2");

        when(client.listAliasesAsync()).thenReturn(Futures.immediateFuture(List.of()));
        assertThat(switcher.resolveCurrent("vec")).isNull();
    }

    @Test
    void drop_refusesWhenCollectionStillAliased() {
        when(client.listCollectionAliasesAsync("vec-g1"))
                .thenReturn(Futures.immediateFuture(List.of("vec")));

        assertThatThrownBy(() -> switcher.drop("vec-g1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vec-g1");
        verify(client, never()).deleteCollectionAsync("vec-g1");
    }

    @Test
    void drop_deletesWhenNoAliasPointsToIt() {
        when(client.listCollectionAliasesAsync("vec-g1"))
                .thenReturn(Futures.immediateFuture(List.of()));
        when(client.deleteCollectionAsync("vec-g1"))
                .thenReturn(Futures.immediateFuture(CollectionOperationResponse.newBuilder().build()));

        switcher.drop("vec-g1");

        verify(client).deleteCollectionAsync("vec-g1");
    }

    @Test
    void prepare_zeroDimension_isConfigError() {
        QdrantCollectionSwitcher zeroDim = new QdrantCollectionSwitcher(client, "vec", 0, "cosine", 5);
        assertThatThrownBy(() -> zeroDim.prepare("vec-g1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vector-dimension");
    }

    @Test
    void prepare_createsCollectionWithConfiguredDimensionAndDistance() {
        when(client.collectionExistsAsync("vec-g1")).thenReturn(Futures.immediateFuture(false));
        when(client.createCollectionAsync(eq("vec-g1"), org.mockito.ArgumentMatchers.any(VectorParams.class)))
                .thenReturn(Futures.immediateFuture(CollectionOperationResponse.newBuilder().build()));

        switcher.prepare("vec-g1");

        var captor = forClass(VectorParams.class);
        verify(client).createCollectionAsync(eq("vec-g1"), captor.capture());
        assertThat(captor.getValue().getSize()).isEqualTo(768L);
        assertThat(captor.getValue().getDistance()).isEqualTo(Distance.Cosine);
    }

    @Test
    void prepare_existingCollection_isIdempotent() {
        when(client.collectionExistsAsync("vec-g1")).thenReturn(Futures.immediateFuture(true));

        switcher.prepare("vec-g1");

        verify(client, never()).createCollectionAsync(eq("vec-g1"),
                org.mockito.ArgumentMatchers.any(VectorParams.class));
    }

    @Test
    void executionException_isUnwrappedWithCollectionName() {
        when(client.collectionExistsAsync("vec-g1"))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("qdrant down")));

        assertThatThrownBy(() -> switcher.exists("vec-g1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("vec-g1")
                .hasMessageContaining("qdrant down");
    }

    @Test
    void unsupportedDistance_isConfigError() {
        assertThatThrownBy(() -> new QdrantCollectionSwitcher(client, "vec", 768, "manhattan", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vector-distance");
    }
}
