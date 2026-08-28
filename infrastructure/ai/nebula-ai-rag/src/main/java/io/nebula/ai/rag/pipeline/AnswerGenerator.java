package io.nebula.ai.rag.pipeline;

/**
 * 答案生成端口
 * <p>
 * 单独抽成端口而不是让管线直接调 {@code ChatService}：应用往往需要在生成环节
 * 保留自己的场景路由、预算隔离、失败语义，把这些塞进框架管线只会让两边都难维护。
 * 应用提供自己的实现覆盖框架默认实现即可。
 * <p>
 * <b>超时由管线兜底：</b>管线会在虚拟线程里跑本方法并按 {@code timeoutMillis} 截断，
 * 因此实现里不做超时控制也不会拖死请求；传入的时长供实现同步设置自己的客户端超时。
 *
 * @author Nebula Framework
 * @since 2.1.1
 */
@FunctionalInterface
public interface AnswerGenerator {

    /**
     * 生成答案
     *
     * @param prompt        已渲染好的提示词
     * @param timeoutMillis 管线给出的超时预算（毫秒）
     * @return 生成的答案
     */
    String generate(String prompt, long timeoutMillis);
}
