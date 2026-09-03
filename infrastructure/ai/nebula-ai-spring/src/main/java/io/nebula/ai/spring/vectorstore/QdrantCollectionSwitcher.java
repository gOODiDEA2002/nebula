package io.nebula.ai.spring.vectorstore;

import com.google.common.util.concurrent.ListenableFuture;
import io.nebula.ai.rag.index.CollectionSwitcher;
import io.nebula.ai.rag.index.VectorStoreIndexSink;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.AliasDescription;
import io.qdrant.client.grpc.Collections.AliasOperations;
import io.qdrant.client.grpc.Collections.CreateAlias;
import io.qdrant.client.grpc.Collections.DeleteAlias;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于 {@link QdrantClient} 的向量集合蓝绿切换器（R3 §4.2，供应商适配落 nebula-ai-spring 守 Y5）
 * <p>
 * 一个实例绑定一个逻辑别名（{@code aliasName}）与新建集合的维度/距离：
 * <ul>
 *   <li>{@code prepare}：集合不存在才建（{@code VectorParams{size, distance}}）；<b>维度显式配 {@code >0}</b>，
 *       不从旧集合推断（换模型换维度是重灌的典型动因）；为 0 报配置错误；</li>
 *   <li>{@code switchTo}：{@code updateAliasesAsync} 一次提交 {@code [DeleteAlias?, CreateAlias]}，
 *       先 {@code listAliasesAsync} 判断别名是否存在以决定是否带 DeleteAlias（对不存在的别名发 Delete 会报错）；</li>
 *   <li>{@code resolveCurrent}：{@code listAliasesAsync} 过滤取 {@code collectionName}；</li>
 *   <li>{@code drop}：先 {@code listCollectionAliasesAsync}，非空则拒绝删除并抛出含别名清单的异常。</li>
 * </ul>
 * 所有异步调用 {@code .get(timeout)} 同步化，{@link ExecutionException} 拆包为含集合名的明确异常。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public class QdrantCollectionSwitcher implements CollectionSwitcher {

    /** 与 {@link VectorStoreIndexSink#NAME} 对齐，用于配对写目标 */
    public static final String NAME = VectorStoreIndexSink.NAME;

    private final QdrantClient client;
    private final String aliasName;
    private final int vectorDimension;
    private final Distance distance;
    private final long timeoutSeconds;

    public QdrantCollectionSwitcher(QdrantClient client, String aliasName, int vectorDimension,
                                    String vectorDistance, long timeoutSeconds) {
        if (client == null) {
            throw new IllegalArgumentException("QdrantClient 不能为空");
        }
        if (aliasName == null || aliasName.isBlank()) {
            throw new IllegalArgumentException("向量别名(aliasName)不能为空");
        }
        this.client = client;
        this.aliasName = aliasName;
        this.vectorDimension = vectorDimension;
        this.distance = parseDistance(vectorDistance);
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void prepare(String physicalName) {
        requireNotAlias(physicalName);
        if (vectorDimension <= 0) {
            throw new IllegalStateException(
                    "nebula.ai.rag.indexing.reindex.vector-dimension 必须显式配 >0 才能建向量集合 "
                            + physicalName + "; 不从旧集合推断维度(换模型换维度是重灌的典型动因)");
        }
        boolean exists = await(client.collectionExistsAsync(physicalName), physicalName);
        if (!exists) {
            VectorParams params = VectorParams.newBuilder()
                    .setSize(vectorDimension)
                    .setDistance(distance)
                    .build();
            await(client.createCollectionAsync(physicalName, params), physicalName);
        }
    }

    @Override
    public boolean exists(String physicalName) {
        return await(client.collectionExistsAsync(physicalName), physicalName);
    }

    @Override
    public void switchTo(String logicalName, String physicalName) {
        requireNotAlias(physicalName);
        List<AliasDescription> aliases = await(client.listAliasesAsync(), physicalName);
        boolean aliasExists = aliases.stream()
                .anyMatch(a -> a.getAliasName().equals(logicalName));

        List<AliasOperations> operations = new ArrayList<>(2);
        if (aliasExists) {
            operations.add(AliasOperations.newBuilder()
                    .setDeleteAlias(DeleteAlias.newBuilder().setAliasName(logicalName).build())
                    .build());
        }
        operations.add(AliasOperations.newBuilder()
                .setCreateAlias(CreateAlias.newBuilder()
                        .setCollectionName(physicalName)
                        .setAliasName(logicalName)
                        .build())
                .build());
        await(client.updateAliasesAsync(operations), physicalName);
    }

    @Override
    public String resolveCurrent(String logicalName) {
        List<AliasDescription> aliases = await(client.listAliasesAsync(), logicalName);
        return aliases.stream()
                .filter(a -> a.getAliasName().equals(logicalName))
                .map(AliasDescription::getCollectionName)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void drop(String physicalName) {
        List<String> pointingAliases = await(client.listCollectionAliasesAsync(physicalName),
                physicalName);
        if (pointingAliases != null && !pointingAliases.isEmpty()) {
            throw new IllegalStateException("拒绝删除向量集合 " + physicalName
                    + ": 仍被别名指向 " + pointingAliases);
        }
        await(client.deleteCollectionAsync(physicalName), physicalName);
    }

    private void requireNotAlias(String physicalName) {
        if (aliasName.equals(physicalName)) {
            throw new IllegalArgumentException("向量物理集合名不能等于别名 " + aliasName
                    + ": 别名与集合不能同名, 启用重灌前需先改名迁移");
        }
    }

    private <T> T await(ListenableFuture<T> future, String collection) {
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qdrant 操作被中断(集合 " + collection + ")", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException(
                    "Qdrant 操作失败(集合 " + collection + "): " + cause.getMessage(), cause);
        } catch (TimeoutException e) {
            throw new IllegalStateException("Qdrant 操作超时(集合 " + collection + ")", e);
        }
    }

    private static Distance parseDistance(String value) {
        if (value == null || value.isBlank()) {
            return Distance.Cosine;
        }
        switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "cosine":
                return Distance.Cosine;
            case "dot":
                return Distance.Dot;
            case "euclid":
                return Distance.Euclid;
            default:
                throw new IllegalArgumentException(
                        "不支持的 vector-distance: " + value + "; 仅支持 cosine|dot|euclid");
        }
    }
}
