package io.nebula.example.microservice.api.user;

import io.nebula.rpc.core.annotation.RpcCall;
import io.nebula.rpc.core.annotation.RpcClient;

import java.util.List;

/**
 * 用户服务 RPC 接口
 * 
 * RPC 接口设计原则：
 * - 参数使用具体类型
 * - 返回值使用业务对象
 * - 不使用 HTTP 路径注解（框架自动处理）
 * - 错误通过 BusinessException 抛出
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@RpcClient("user-service")
public interface UserRpcClient {

    /**
     * 创建用户
     * 
     * @param request 创建用户请求
     * @return 创建的用户信息
     */
    @RpcCall
    UserDto createUser(CreateUserRequest request);

    /**
     * 根据 ID 获取用户
     * 
     * @param userId 用户 ID
     * @return 用户信息，不存在返回 null
     */
    @RpcCall
    UserDto getUserById(Long userId);

    /**
     * 获取用户列表
     * 
     * @return 用户列表
     */
    @RpcCall
    List<UserDto> listUsers();
}
