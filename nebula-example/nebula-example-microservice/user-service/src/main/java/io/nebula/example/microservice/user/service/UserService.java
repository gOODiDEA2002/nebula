package io.nebula.example.microservice.user.service;

import io.nebula.example.microservice.api.user.CreateUserRequest;
import io.nebula.example.microservice.api.user.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务实现
 * 
 * 使用内存存储模拟数据库操作
 * 
 * @author Nebula Framework Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class UserService {

    private final Map<Long, UserDto> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserService() {
        // 初始化一些测试数据
        createUser(CreateUserRequest.builder()
                .username("alice")
                .email("alice@example.com")
                .phone("13800138001")
                .build());
        createUser(CreateUserRequest.builder()
                .username("bob")
                .email("bob@example.com")
                .phone("13800138002")
                .build());
        createUser(CreateUserRequest.builder()
                .username("charlie")
                .email("charlie@example.com")
                .phone("13800138003")
                .build());
        log.info("初始化用户数据完成，共 {} 条", userStore.size());
    }

    /**
     * 创建用户
     */
    public UserDto createUser(CreateUserRequest request) {
        Long id = idGenerator.getAndIncrement();
        UserDto user = UserDto.builder()
                .id(id)
                .username(request.getUsername())
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
        userStore.put(id, user);
        log.info("创建用户: id={}, username={}", id, user.getUsername());
        return user;
    }

    /**
     * 根据 ID 获取用户
     */
    public UserDto getUserById(Long userId) {
        UserDto user = userStore.get(userId);
        log.debug("查询用户: id={}, found={}", userId, user != null);
        return user;
    }

    /**
     * 获取所有用户
     */
    public List<UserDto> listUsers() {
        log.debug("查询所有用户，共 {} 条", userStore.size());
        return userStore.values().stream().toList();
    }
}
