package io.nebula.ai.spring.vectorstore;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 业务 docId 到 Qdrant 点 ID 的确定性映射
 * <p>
 * <b>为什么需要：</b>Qdrant 的点 ID 只接受 UUID 或无符号整数两种形态，而业务 docId
 * 常常是命名空间字符串（{@code mc:0113120012}、{@code product-1480334}、
 * {@code enterprise-123}）。直接写入会在
 * {@code QdrantVectorStore#doAdd} 抛 {@code IllegalArgumentException: Invalid UUID string}，
 * 表现为灌库逐条失败、集合永远是空的。
 * <p>
 * <b>映射规则：</b>RFC 4122 的 name-based UUIDv5（SHA-1），命名空间由
 * {@code uuid5(DNS, namespaceName)} 推导。因此同一个 docId 恒得同一个点 ID —— 幂等重写落在
 * 同一个点上做 upsert，不会像随机 ID 那样每写一次多一个重复点；按 docId 删除也只要走同一映射即可命中。
 * <p>
 * <b>命名空间是数据资产的一部分。</b>换掉 {@code namespaceName} 等于把全库点 ID 换一遍，
 * 已灌入的数据会全部检索不到、只能全量重灌。因此它是必填构造参数而不是可省缺省值。
 * <p>
 * <b>不用 {@link UUID#nameUUIDFromBytes} 的原因：</b>它产出的是 v3（MD5）且不带命名空间隔离，
 * 换算规则一旦要改就没有区分度。这里的 SHA-1 只作 name-based 摘要用，不承担任何安全职责。
 * <p>
 * <b>运维如何按原始 docId 定位点：</b>点 ID 可离线算出，无需给高基数的原始 docId 字段
 * 建 payload 索引（400 多万条的 keyword 索引常驻内存，代价远高于收益）。一行 Python 即可：
 * <pre>
 * python3 -c "import uuid;ns=uuid.uuid5(uuid.NAMESPACE_DNS,'vector.example.com');print(uuid.uuid5(ns,'mc:0113120012'))"
 * </pre>
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public final class QdrantPointIdMapper {

    /**
     * 原始 docId 在 payload 中的默认字段名
     * <p>
     * 检索回来的 Document ID 是 UUID 点 ID，靠这个字段还原成业务侧认得的 docId。
     * 还原不是可选项：混合检索的 RRF 融合按 ID 对齐向量与关键词两路结果，
     * 关键词那边一直是原始 docId，向量这边若返回 UUID，同一条文档会被当成两条，融合去重直接失效。
     */
    public static final String DEFAULT_ORIGINAL_DOC_ID_FIELD = "orig_doc_id";

    /** RFC 4122 预定义的 DNS 命名空间，仅用于推导实例命名空间 */
    static final UUID DNS_NAMESPACE = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");

    private final String namespaceName;
    private final UUID namespace;

    /**
     * @param namespaceName 推导命名空间的名字，如 {@code vector.sia.vocoor.com}；不可为空
     */
    public QdrantPointIdMapper(String namespaceName) {
        if (namespaceName == null || namespaceName.isBlank()) {
            throw new IllegalArgumentException(
                    "Qdrant 点 ID 映射命名空间不能为空: 缺失会导致全库点 ID 错位, 检索恒为空");
        }
        this.namespaceName = namespaceName;
        this.namespace = uuidV5(DNS_NAMESPACE, namespaceName);
    }

    /**
     * 推导命名空间所用的名字
     */
    public String getNamespaceName() {
        return namespaceName;
    }

    /**
     * 推导出的命名空间 UUID，启动日志打印它供人工与运维脚本核对
     */
    public UUID getNamespace() {
        return namespace;
    }

    /**
     * 业务 docId 到 Qdrant 点 ID（UUID 字符串）
     *
     * @param docId 原始命名空间 docId，如 {@code mc:0113120012}
     * @return 确定性 UUIDv5 字符串
     */
    public String toPointId(String docId) {
        if (docId == null || docId.isBlank()) {
            throw new IllegalArgumentException("docId 不能为空，无法映射 Qdrant 点 ID");
        }
        return uuidV5(namespace, docId).toString();
    }

    /**
     * RFC 4122 §4.3 name-based UUID（version 5, SHA-1）
     */
    static UUID uuidV5(UUID namespace, String name) {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            // SHA-1 是 JRE 必备算法，取不到说明运行环境已不完整
            throw new IllegalStateException("当前 JRE 不支持 SHA-1，无法生成 UUIDv5", e);
        }
        ByteBuffer namespaceBytes = ByteBuffer.allocate(16)
                .putLong(namespace.getMostSignificantBits())
                .putLong(namespace.getLeastSignificantBits());
        sha1.update(namespaceBytes.array());
        sha1.update(name.getBytes(StandardCharsets.UTF_8));

        byte[] hash = sha1.digest();
        hash[6] = (byte) ((hash[6] & 0x0F) | 0x50);  // version 5
        hash[8] = (byte) ((hash[8] & 0x3F) | 0x80);  // variant RFC 4122
        ByteBuffer buffer = ByteBuffer.wrap(hash, 0, 16);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
