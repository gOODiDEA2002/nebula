package io.nebula.rpc.grpc.test;

import io.nebula.rpc.core.annotation.RpcClient;
import java.util.List;

/**
 * 测试用的 RPC 服务接口
 */
@RpcClient(name = "test-service")
public interface TestRpcService {
    
    String sayHello(String name);
    
    Integer add(Integer a, Integer b);
    
    TestUser getUser(Long userId);
    
    /**
     * 获取用户列表 - 用于测试泛型返回类型
     */
    List<TestUser> getUserList();
}

