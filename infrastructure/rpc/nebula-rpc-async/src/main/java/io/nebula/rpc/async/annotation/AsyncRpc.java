package io.nebula.rpc.async.annotation;

import java.lang.annotation.*;

/**
 * 标记RPC方法为异步执行
 * 
 * <p>使用示例：
 * <pre>{@code
 * @RpcClient
 * public interface DataProcessRpcClient {
 *     @AsyncRpc(timeout = 600)  // 异步执行，超时10分钟
 *     AsyncRpcResult<ProcessResult> processData(ProcessRequest request);
 * }
 * }</pre>
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AsyncRpc {
    
    /**
     * 执行超时时间（秒），0表示不限制
     */
    long timeout() default 0;
    
    /**
     * 是否自动重试
     */
    boolean retry() default false;
    
    /**
     * 最大重试次数
     */
    int maxRetries() default 3;
}
