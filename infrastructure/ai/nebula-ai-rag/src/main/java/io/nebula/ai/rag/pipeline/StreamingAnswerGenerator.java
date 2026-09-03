package io.nebula.ai.rag.pipeline;

import reactor.core.publisher.Flux;

/**
 * 流式答案生成端口（R4 §5.1）
 * <p>
 * 只负责产出<b>文本增量</b>：超时、降级、终态、引用事件都由管线统一负责，端口实现不掺和。
 * 单抽象方法，可用 lambda 提供（与 {@code AnswerGenerator} 惯例一致）。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface StreamingAnswerGenerator {

    /**
     * 按提示词流式生成答案增量
     *
     * @param prompt 完整提示词
     * @return 文本增量流；正常结束以 {@code onComplete} 收尾，底层错误以 {@code onError} 传出，
     *         由管线转换为 {@code ERROR}/降级事件
     */
    Flux<String> generateStream(String prompt);
}
