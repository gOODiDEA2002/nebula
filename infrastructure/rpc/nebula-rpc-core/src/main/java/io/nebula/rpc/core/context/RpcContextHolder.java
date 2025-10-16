package io.nebula.rpc.core.context;

/**
 * RPC 上下文持有者
 * 使用 ThreadLocal 传递 RPC 调用上下文信息
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class RpcContextHolder {
    
    private static final ThreadLocal<String> SERVICE_NAME_HOLDER = new ThreadLocal<>();
    
    /**
     * 设置当前线程的服务名
     */
    public static void setServiceName(String serviceName) {
        SERVICE_NAME_HOLDER.set(serviceName);
    }
    
    /**
     * 获取当前线程的服务名
     */
    public static String getServiceName() {
        return SERVICE_NAME_HOLDER.get();
    }
    
    /**
     * 清除当前线程的服务名
     */
    public static void clear() {
        SERVICE_NAME_HOLDER.remove();
    }
}

