package io.nebula.example.user.service.business.impl;

import io.nebula.example.user.api.entity.User;
import io.nebula.example.user.api.dto.*;
import io.nebula.example.user.api.vo.UserVo;
import io.nebula.example.user.service.business.RpcDemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * RPC演示服务实现
 * 使用内存存储演示RPC功能
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
@Slf4j
@Service
public class RpcDemoServiceImpl implements RpcDemoService {
    
    // 使用内存存储用户数据（演示用）
    private final Map<Long, User> userStorage = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    public RpcDemoServiceImpl() {
        // 初始化一些测试数据
        initTestData();
    }
    
    @Override
    public CreateUserDto.Response createUser(CreateUserDto.Request request) {
        log.info("创建用户: username={}", request.getUsername());
        
        User user = new User();
        user.setId(idGenerator.getAndIncrement());
        user.setUsername(request.getUsername());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        
        userStorage.put(user.getId(), user);
        
        CreateUserDto.Response response = new CreateUserDto.Response();
        response.setId(user.getId());
        
        log.info("创建用户成功: id={}", user.getId());
        return response;
    }
    
    @Override
    public GetUserDto.Response getUserById(GetUserDto.Request request) {
        log.info("获取用户详情: id={}", request.getId());
        
        User user = userStorage.get(request.getId());
        
        GetUserDto.Response response = new GetUserDto.Response();
        if (user != null) {
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(user, userVo);
            response.setUser(userVo);
        }
        
        return response;
    }
    
    @Override
    public GetUsersDto.Response getUsers(GetUsersDto.Request request) {
        log.info("获取用户列表: page={}, size={}", request.getPage(), request.getSize());
        
        // 过滤用户
        List<User> filteredUsers = userStorage.values().stream()
            .filter(user -> {
                if (request.getUsername() != null && !user.getUsername().contains(request.getUsername())) {
                    return false;
                }
                if (request.getName() != null && !user.getName().contains(request.getName())) {
                    return false;
                }
                if (request.getStatus() != null && !user.getStatus().equals(request.getStatus())) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
        // 分页
        int page = request.getPage();
        int size = request.getSize();
        int start = (page - 1) * size;
        int end = Math.min(start + size, filteredUsers.size());
        
        List<User> pagedUsers = filteredUsers.subList(
            Math.min(start, filteredUsers.size()),
            end
        );
        
        // 转换为VO
        List<UserVo> userVos = pagedUsers.stream()
            .map(user -> {
                UserVo vo = new UserVo();
                BeanUtils.copyProperties(user, vo);
                return vo;
            })
            .collect(Collectors.toList());
        
        GetUsersDto.Response response = new GetUsersDto.Response();
        response.setUsers(userVos);
        response.setTotal((long) filteredUsers.size());
        response.setPage(page);
        response.setSize(size);
        
        return response;
    }
    
    @Override
    public UpdateUserDto.Response updateUser(UpdateUserDto.Request request) {
        log.info("更新用户: id={}", request.getId());
        
        User user = userStorage.get(request.getId());
        if (user == null) {
            return new UpdateUserDto.Response();
        }
        
        // 更新字段
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        user.setUpdateTime(LocalDateTime.now());
        
        userStorage.put(user.getId(), user);
        
        // 转换为VO
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        
        UpdateUserDto.Response response = new UpdateUserDto.Response();
        response.setUser(userVo);
        
        log.info("更新用户成功: id={}", user.getId());
        return response;
    }
    
    @Override
    public DeleteUserDto.Response deleteUser(DeleteUserDto.Request request) {
        log.info("删除用户: id={}", request.getId());
        
        User removedUser = userStorage.remove(request.getId());
        
        DeleteUserDto.Response response = new DeleteUserDto.Response();
        response.setSuccess(removedUser != null);
        
        log.info("删除用户{}: id={}", removedUser != null ? "成功" : "失败", request.getId());
        return response;
    }
    
    /**
     * 初始化测试数据
     */
    private void initTestData() {
        for (int i = 1; i <= 10; i++) {
            User user = new User();
            user.setId((long) i);
            user.setUsername("user" + i);
            user.setName("测试用户" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPhone("1380000000" + i);
            user.setStatus(i % 3 == 0 ? "INACTIVE" : "ACTIVE");
            user.setCreateTime(LocalDateTime.now().minusDays(i));
            user.setUpdateTime(LocalDateTime.now().minusDays(i));
            
            userStorage.put(user.getId(), user);
            idGenerator.set(i + 1);
        }
        log.info("初始化了 {} 个测试用户", userStorage.size());
    }
}

