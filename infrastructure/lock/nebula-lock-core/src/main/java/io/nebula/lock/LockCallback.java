package io.nebula.lock;

/**
 * 锁回调接口
 * 
 * 用于简化锁的使用,自动处理锁的获取和释放
 * 
 * 使用示例:
 * <pre>{@code
 * lockManager.execute("order:1001", () -> {
 *     // 业务逻辑
 *     return orderService.process(orderId);
 * });
 * }</pre>
 *
 * @param <T> 返回值类型
 * @author Nebula Framework
 * @since 2.0.0
 */
@FunctionalInterface
public interface LockCallback<T> {
    
    /**
     * 在持有锁的情况下执行业务逻辑
     * 
     * @return 执行结果
     * @throws Exception 业务异常
     */
    T execute() throws Exception;
}

