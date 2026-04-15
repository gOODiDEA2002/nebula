package io.nebula.example.user.service.controller;

import io.nebula.example.user.api.dto.*;
import io.nebula.example.user.service.business.RpcDemoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 提供 HTTP REST API 端点供直接测试
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/rpc/users")
@RequiredArgsConstructor
public class UserController {
    
    private final RpcDemoService rpcDemoService;
    
    /**
     * 创建用户
     */
    @PostMapping
    public CreateUserDto.Response createUser(@Valid @RequestBody CreateUserDto.Request request) {
        log.info("REST API: createUser, username={}", request.getUsername());
        return rpcDemoService.createUser(request);
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/{id}")
    public GetUserDto.Response getUserById(@PathVariable Long id) {
        log.info("REST API: getUserById, id={}", id);
        GetUserDto.Request request = new GetUserDto.Request();
        request.setId(id);
        return rpcDemoService.getUserById(request);
    }
    
    /**
     * 获取用户列表
     */
    @GetMapping
    public GetUsersDto.Response getUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("REST API: getUsers, page={}, size={}", page, size);
        
        GetUsersDto.Request request = new GetUsersDto.Request();
        request.setUsername(username);
        request.setName(name);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);
        
        return rpcDemoService.getUsers(request);
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public UpdateUserDto.Response updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserDto.Request request) {
        log.info("REST API: updateUser, id={}", id);
        request.setId(id);
        return rpcDemoService.updateUser(request);
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public DeleteUserDto.Response deleteUser(@PathVariable Long id) {
        log.info("REST API: deleteUser, id={}", id);
        DeleteUserDto.Request request = new DeleteUserDto.Request();
        request.setId(id);
        return rpcDemoService.deleteUser(request);
    }
}

