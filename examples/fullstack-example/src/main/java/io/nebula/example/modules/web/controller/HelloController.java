package io.nebula.example.modules.web.controller;

import io.nebula.core.common.result.Result;
import io.nebula.web.controller.BaseController;
import io.nebula.web.mask.MaskType;
import io.nebula.web.mask.SensitiveData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Hello控制器
 * 演示 Nebula Web 的各种功能：缓存、性能监控、数据脱敏等
 */
@Slf4j
@RestController
@RequestMapping("/hello")
public class HelloController extends BaseController {
    
    private final AtomicLong requestCounter = new AtomicLong(0);
    private final Map<String, Object> cache = new HashMap<>();

    /**
     * 基础 Hello 接口
     * 演示基本响应和请求日志
     */
    @GetMapping
    public Result<Map<String, Object>> hello() {
        log.info("收到hello请求");
        
        long requestId = requestCounter.incrementAndGet();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from Nebula Framework!");
        response.put("timestamp", LocalDateTime.now());
        response.put("status", "success");
        response.put("framework", "Nebula 2.0.1-SNAPSHOT");
        response.put("requestId", requestId);
        response.put("features", Arrays.asList(
            "认证系统", "限流控制", "响应缓存", "性能监控", 
            "健康检查", "数据脱敏", "请求日志", "全局异常处理"
        ));
        
        return success(response);
    }

    /**
     * 缓存演示接口
     * GET 请求会被自动缓存（如果启用了缓存功能）
     */
    @GetMapping("/cached-data/{id}")
    public Result<CachedDataResponse> getCachedData(@PathVariable String id) {
        log.info("获取缓存数据，ID: {}", id);
        
        // 模拟数据库查询延迟
        try {
            Thread.sleep(1000); // 1秒延迟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        CachedDataResponse data = new CachedDataResponse();
        data.setId(id);
        data.setData("这是ID为 " + id + " 的缓存数据");
        data.setGeneratedAt(LocalDateTime.now());
        data.setFromCache(false); // 首次生成
        
        return success(data);
    }

    /**
     * 慢请求演示
     * 用于测试性能监控的慢请求检测
     */
    @GetMapping("/slow")
    public Result<Map<String, Object>> slowRequest(@RequestParam(defaultValue = "2000") int delayMs) {
        log.info("执行慢请求，延迟: {}ms", delayMs);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "慢请求执行完成");
        response.put("requestedDelay", delayMs);
        response.put("actualDelay", endTime - startTime);
        response.put("timestamp", LocalDateTime.now());
        
        return success(response);
    }

    /**
     * 数据脱敏演示
     * 演示敏感数据的自动脱敏功能
     */
    @GetMapping("/sensitive-data")
    public Result<SensitiveDataResponse> getSensitiveData() {
        log.info("获取敏感数据");
        
        SensitiveDataResponse response = new SensitiveDataResponse();
        response.setName("张三");
        response.setEmail("zhangsan@example.com");
        response.setMobile("13888888888");
        response.setIdCard("110101199001011234");
        response.setBankCard("6222600260001234567");
        response.setAddress("北京市朝阳区建国门外大街1号");
        response.setPassword("mySecretPassword123");
        response.setIpAddress("192.168.1.100");
        
        return success(response);
    }

    /**
     * 限流测试接口
     * 可以用来测试限流功能
     */
    @GetMapping("/rate-limit-test")
    public Result<Map<String, Object>> rateLimitTest() {
        log.info("限流测试请求");
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "请求成功通过限流检查");
        response.put("timestamp", LocalDateTime.now());
        response.put("tip", "请快速多次调用此接口来测试限流功能");
        
        return success(response);
    }

    /**
     * 错误演示接口
     * 用于测试全局异常处理
     */
    @GetMapping("/error")
    public Result<String> errorDemo(@RequestParam(defaultValue = "business") String type) {
        log.info("错误演示，类型: {}", type);
        
        switch (type) {
            case "business":
                return error("BUSINESS_ERROR", "这是一个业务异常演示");
            case "runtime":
                throw new RuntimeException("这是一个运行时异常演示");
            case "null":
                String nullString = null;
                return success(nullString.toString()); // 会抛出 NPE
            default:
                return error("UNKNOWN_ERROR_TYPE", "未知的错误类型: " + type);
        }
    }

    /**
     * 批量数据接口
     * 用于测试性能和缓存
     */
    @GetMapping("/batch-data")
    public Result<List<BatchDataItem>> getBatchData(@RequestParam(defaultValue = "10") int count) {
        log.info("获取批量数据，数量: {}", count);
        
        List<BatchDataItem> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BatchDataItem item = new BatchDataItem();
            item.setId((long) i);
            item.setName("Item " + i);
            item.setDescription("这是第 " + i + " 个数据项");
            item.setCreatedAt(LocalDateTime.now().minusDays(i));
            items.add(item);
        }
        
        return success(items);
    }

    /**
     * 健康检查相关信息
     */
    @GetMapping("/system-info")
    public Result<Map<String, Object>> getSystemInfo() {
        log.info("获取系统信息");
        
        Runtime runtime = Runtime.getRuntime();
        
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("javaVersion", System.getProperty("java.version"));
        systemInfo.put("osName", System.getProperty("os.name"));
        systemInfo.put("osVersion", System.getProperty("os.version"));
        systemInfo.put("totalMemory", runtime.totalMemory());
        systemInfo.put("freeMemory", runtime.freeMemory());
        systemInfo.put("maxMemory", runtime.maxMemory());
        systemInfo.put("availableProcessors", runtime.availableProcessors());
        systemInfo.put("timestamp", LocalDateTime.now());
        
        return success(systemInfo);
    }

    /**
     * POST 请求演示
     * POST 请求不会被缓存
     */
    @PostMapping("/submit")
    public Result<Map<String, Object>> submitData(@RequestBody Map<String, Object> data) {
        log.info("接收到POST数据: {}", data);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "数据提交成功");
        response.put("receivedData", data);
        response.put("processedAt", LocalDateTime.now());
        response.put("dataSize", data.size());
        
        return success(response);
    }

    // DTO 类定义

    public static class CachedDataResponse {
        private String id;
        private String data;
        private LocalDateTime generatedAt;
        private boolean fromCache;

        // getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public boolean isFromCache() { return fromCache; }
        public void setFromCache(boolean fromCache) { this.fromCache = fromCache; }
    }

    public static class SensitiveDataResponse {
        @SensitiveData(type = MaskType.NAME)
        private String name;
        
        @SensitiveData(type = MaskType.EMAIL)
        private String email;
        
        @SensitiveData(type = MaskType.PHONE)
        private String mobile;
        
        @SensitiveData(type = MaskType.ID_CARD)
        private String idCard;
        
        @SensitiveData(type = MaskType.BANK_CARD)
        private String bankCard;
        
        @SensitiveData(type = MaskType.ADDRESS)
        private String address;
        
        @SensitiveData(type = MaskType.PASSWORD)
        private String password;
        
        @SensitiveData(type = MaskType.IP_ADDRESS)
        private String ipAddress;

        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getMobile() { return mobile; }
        public void setMobile(String mobile) { this.mobile = mobile; }
        public String getIdCard() { return idCard; }
        public void setIdCard(String idCard) { this.idCard = idCard; }
        public String getBankCard() { return bankCard; }
        public void setBankCard(String bankCard) { this.bankCard = bankCard; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }

    public static class BatchDataItem {
        private Long id;
        private String name;
        private String description;
        private LocalDateTime createdAt;

        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
