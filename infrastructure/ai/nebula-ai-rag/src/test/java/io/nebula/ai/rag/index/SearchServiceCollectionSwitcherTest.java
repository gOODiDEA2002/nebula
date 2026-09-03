package io.nebula.ai.rag.index;

import io.nebula.search.core.SearchService;
import io.nebula.search.core.model.IndexResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BM25 切换器端口契约（R3 §3.1、§4.1）
 * <p>
 * prepare 幂等、drop 拒删被别名指向者、resolveCurrent 空值语义，全部经有状态的
 * {@link SearchService} 桩验证（桩不比真实实现宽松：alias/索引状态真实变化）。
 */
class SearchServiceCollectionSwitcherTest {

    private Set<String> indices;
    private Map<String, String> aliasToIndex;
    private SearchService searchService;
    private SearchServiceCollectionSwitcher switcher;

    @BeforeEach
    void setUp() {
        indices = new HashSet<>();
        aliasToIndex = new HashMap<>();
        searchService = mock(SearchService.class);
        when(searchService.indexExists(anyString()))
                .thenAnswer(i -> indices.contains(i.<String>getArgument(0)));
        when(searchService.createIndex(anyString(), any())).thenAnswer(i -> {
            indices.add(i.getArgument(0));
            return IndexResult.success(i.getArgument(0));
        });
        when(searchService.deleteIndex(anyString())).thenAnswer(i -> {
            indices.remove(i.<String>getArgument(0));
            return IndexResult.success(i.getArgument(0));
        });
        when(searchService.switchAlias(anyString(), anyString())).thenAnswer(i -> {
            aliasToIndex.put(i.getArgument(0), i.getArgument(1));
            return IndexResult.success(i.getArgument(0));
        });
        when(searchService.resolveAlias(anyString())).thenAnswer(i -> {
            String alias = i.getArgument(0);
            return aliasToIndex.containsKey(alias) ? List.of(aliasToIndex.get(alias)) : List.of();
        });
        switcher = new SearchServiceCollectionSwitcher(searchService, "chunks", "standard", "standard");
    }

    @Test
    void name_alignsWithSink() {
        assertThat(switcher.name()).isEqualTo(SearchServiceIndexSink.NAME);
    }

    @Test
    void physicalName_defaultsToLogicalWithGeneration() {
        assertThat(switcher.physicalName("chunks", 3)).isEqualTo("chunks-g3");
    }

    @Test
    void prepare_isIdempotent_createsOnlyWhenAbsent() {
        switcher.prepare("chunks-g1");
        switcher.prepare("chunks-g1");   // 第二次已存在, 不重建

        assertThat(switcher.exists("chunks-g1")).isTrue();
        verify(searchService, times(1)).createIndex(anyString(), any());
    }

    @Test
    void resolveCurrent_nullWhenAliasAbsent_thenTargetAfterSwitch() {
        assertThat(switcher.resolveCurrent("chunks")).isNull();

        switcher.prepare("chunks-g1");
        switcher.switchTo("chunks", "chunks-g1");

        assertThat(switcher.resolveCurrent("chunks")).isEqualTo("chunks-g1");
    }

    @Test
    void drop_refusesTargetStillPointedToByAlias() {
        switcher.prepare("chunks-g1");
        switcher.switchTo("chunks", "chunks-g1");

        assertThatThrownBy(() -> switcher.drop("chunks-g1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("chunks-g1");
        assertThat(switcher.exists("chunks-g1")).isTrue();
    }

    @Test
    void drop_removesInactiveTarget() {
        switcher.prepare("chunks-g1");
        switcher.prepare("chunks-g2");
        switcher.switchTo("chunks", "chunks-g2");

        switcher.drop("chunks-g1");   // g1 非活动, 允许删

        assertThat(switcher.exists("chunks-g1")).isFalse();
        assertThat(switcher.exists("chunks-g2")).isTrue();
    }

    @Test
    void prepare_rejectsPhysicalNameEqualToAlias() {
        assertThatThrownBy(() -> switcher.prepare("chunks"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("别名");
    }

    @Test
    void targetFactory_producesSinkAlignedWithSwitcherName() {
        SearchServiceIndexTargetFactory factory =
                new SearchServiceIndexTargetFactory(searchService, "standard", "standard");
        assertThat(factory.name()).isEqualTo(SearchServiceIndexSink.NAME);
        IndexSink sink = factory.sinkFor("chunks-g2");
        assertThat(sink).isNotNull();
        assertThat(sink.name()).isEqualTo(SearchServiceIndexSink.NAME);
    }
}
