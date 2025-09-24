package io.nebula.rpc.core.client;

import java.util.concurrent.CompletableFuture;

/**
 * RPC客户端接口
 * 提供同步和异步调用能力
 */
public interface RpcClient {
    
    /**
     * 同步调用
     * 
     * @param serviceClass 服务接口类
     * @param methodName   方法名
     * @param args         参数
     * @param <T>          返回类型
     * @return 调用结果
     */
    <T> T call(Class<T> serviceClass, String methodName, Object... args);
    
    /**
     * 异步调用
     * 
     * @param serviceClass 服务接口类
     * @param methodName   方法名
     * @param args         参数
     * @param <T>          返回类型
     * @return Future结果
     */
    <T> CompletableFuture<T> callAsync(Class<T> serviceClass, String methodName, Object... args);
    
    /**
     * 创建服务代理
     * 
     * @param serviceClass 服务接口类
     * @param <T>          服务类型
     * @return 服务代理对象
     */
    <T> T createProxy(Class<T> serviceClass);
    
    /**
     * 获取服务地址
     * 
     * @param serviceName 服务名称
     * @return 服务地址
     */
    String getServiceAddress(String serviceName);
    
    /**
     * 关闭客户端
     */
    void close();
}
