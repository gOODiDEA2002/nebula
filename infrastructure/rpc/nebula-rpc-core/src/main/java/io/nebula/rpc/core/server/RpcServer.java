package io.nebula.rpc.core.server;

/**
 * RPC服务器接口
 * 提供服务注册和生命周期管理
 */
public interface RpcServer {
    
    /**
     * 注册服务
     * 
     * @param serviceClass 服务接口类
     * @param serviceImpl  服务实现
     * @param <T>          服务类型
     */
    <T> void registerService(Class<T> serviceClass, T serviceImpl);
    
    /**
     * 注册服务（带名称）
     * 
     * @param serviceName  服务名称
     * @param serviceClass 服务接口类
     * @param serviceImpl  服务实现
     * @param <T>          服务类型
     */
    <T> void registerService(String serviceName, Class<T> serviceClass, T serviceImpl);
    
    /**
     * 启动服务器
     * 
     * @param port 端口号
     */
    void start(int port);
    
    /**
     * 关闭服务器
     */
    void shutdown();
    
    /**
     * 获取服务器状态
     * 
     * @return 是否运行中
     */
    boolean isRunning();
    
    /**
     * 获取服务器端口
     * 
     * @return 端口号
     */
    int getPort();
}
