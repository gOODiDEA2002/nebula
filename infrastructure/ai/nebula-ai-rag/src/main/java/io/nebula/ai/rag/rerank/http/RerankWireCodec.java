package io.nebula.ai.rag.rerank.http;

import java.util.List;

/**
 * 交叉编码重排服务的 wire 格式编解码端口（R4 §3.1）
 * <p>
 * 请求编码与响应解码分离，便于加第三种格式而不改重排器：本接口公开且非 final，
 * 使用方实现后以自定义 {@code Reranker} Bean 组装即可，无需改 nebula。
 * <p>
 * JSON 编解码统一走 {@code io.nebula.core.common.util.JsonUtils}（Jackson 3），
 * 不引入新序列化器。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
public interface RerankWireCodec {

    /**
     * 编码一批候选为请求体 JSON
     *
     * @param query 查询文本
     * @param texts 候选正文（已按批切分、已按需截断）
     * @param model 模型名；部分格式（如 Cohere）必填，部分（如 TEI）忽略
     * @return 请求体 JSON 字符串
     */
    String encode(String query, List<String> texts, String model);

    /**
     * 解码响应为 (index, score) 列表
     * <p>
     * 条数、索引范围由调用方（重排器）校验并按 {@code mismatch} 直通处理；
     * 本方法只负责结构解析，遇到非 JSON 或缺字段时抛异常，由调用方按 {@code decode-error} 处理。
     *
     * @param responseBody 响应体
     * @return 每条候选的批内下标与分数
     */
    List<ScoredIndex> decode(String responseBody);

    /**
     * 单条候选的批内下标与交叉编码分
     *
     * @param index 批内下标（相对本批第一条，从 0 起）
     * @param score 交叉编码相关性分
     */
    record ScoredIndex(int index, double score) {
    }
}
