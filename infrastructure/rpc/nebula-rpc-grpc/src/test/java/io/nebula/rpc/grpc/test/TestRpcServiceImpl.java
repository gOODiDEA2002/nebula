package io.nebula.rpc.grpc.test;

import io.nebula.rpc.core.annotation.RpcService;
import java.util.Arrays;
import java.util.List;

/**
 * 测试用的 RPC 服务实现
 */
@RpcService
public class TestRpcServiceImpl implements TestRpcService {
    
    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }
    
    @Override
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
    
    @Override
    public TestUser getUser(Long userId) {
        return new TestUser(userId, "User" + userId, 25);
    }
    
    @Override
    public List<TestUser> getUserList() {
        return Arrays.asList(
            new TestUser(1L, "Alice", 25),
            new TestUser(2L, "Bob", 30),
            new TestUser(3L, "Charlie", 35)
        );
    }
}

