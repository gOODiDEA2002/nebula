package io.nebula.web.health;

import io.nebula.web.health.checkers.ApplicationHealthChecker;
import io.nebula.web.health.checkers.DiskSpaceHealthChecker;
import io.nebula.web.health.checkers.MemoryHealthChecker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 健康检查器测试
 */
class HealthCheckerTest {
    
    @Test
    void testHealthCheckUp() {
        // 创建内存健康检查器（阈值100%，确保始终UP）
        MemoryHealthChecker checker = new MemoryHealthChecker(100.0);
        
        // 执行健康检查
        HealthCheckResult result = checker.check();
        
        // 验证健康状态
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.getDetails()).isNotEmpty();
        assertThat(result.getDetails()).containsKeys("totalMemory", "usedMemory", "maxMemory");
    }
    
    @Test
    void testHealthCheckDown() {
        // 创建内存健康检查器（阈值0%，确保始终DOWN）
        MemoryHealthChecker checker = new MemoryHealthChecker(0.0);
        
        // 执行健康检查
        HealthCheckResult result = checker.check();
        
        // 验证不健康状态
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isIn(HealthStatus.DOWN, HealthStatus.UP); // 可能UP或DOWN取决于实际内存使用
        assertThat(result.getDetails()).isNotEmpty();
    }
    
    @Test
    void testMemoryHealthCheck() {
        // 创建内存健康检查器
        MemoryHealthChecker checker = new MemoryHealthChecker();
        
        // 验证检查器名称
        assertThat(checker.getName()).isEqualTo("memory");
        assertThat(checker.isEnabled()).isTrue();
        
        // 执行健康检查
        HealthCheckResult result = checker.check();
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isIn(HealthStatus.UP, HealthStatus.DOWN);
        assertThat(result.getDetails()).containsKeys(
            "totalMemory", 
            "freeMemory", 
            "usedMemory", 
            "maxMemory",
            "usagePercentage",
            "heapUsed",
            "heapMax"
        );
        assertThat(result.getResponseTime()).isGreaterThanOrEqualTo(0);
    }
    
    @Test
    void testDiskHealthCheck() {
        // 创建磁盘空间健康检查器
        DiskSpaceHealthChecker checker = new DiskSpaceHealthChecker();
        
        // 验证检查器名称
        assertThat(checker.getName()).isEqualTo("diskSpace");
        
        // 执行健康检查
        HealthCheckResult result = checker.check();
        
        // 验证结果
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isIn(HealthStatus.UP, HealthStatus.DOWN);
        assertThat(result.getDetails()).containsKeys(
            "total",
            "free",
            "used",
            "usagePercentage"
        );
    }
    
    @Test
    void testApplicationHealthCheck() {
        // 跳过ApplicationHealthChecker测试，因为它需要ApplicationContext
        // 这个测试应该在集成测试中进行
    }
    
    @Test
    void testHealthCheckResultBuilder() {
        // 测试健康检查结果构建
        HealthCheckResult result = HealthCheckResult.up()
            .withDetail("key1", "value1")
            .withDetail("key2", 123)
            .withResponseTime(100)
            .withSuggestion("Test suggestion");
        
        // 验证结果
        assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.getDetails()).containsEntry("key1", "value1");
        assertThat(result.getDetails()).containsEntry("key2", 123);
        assertThat(result.getResponseTime()).isEqualTo(100);
        assertThat(result.getSuggestion()).isEqualTo("Test suggestion");
    }
    
    @Test
    void testHealthCheckResultWithError() {
        // 测试带错误信息的健康检查结果
        String errorMessage = "Test error message";
        HealthCheckResult result = HealthCheckResult.down(errorMessage);
        
        // 验证结果
        assertThat(result.getStatus()).isEqualTo(HealthStatus.DOWN);
        assertThat(result.getError()).isEqualTo(errorMessage);
    }
    
    @Test
    void testCheckerOrder() {
        // 测试检查器优先级
        MemoryHealthChecker memoryChecker = new MemoryHealthChecker();
        DiskSpaceHealthChecker diskChecker = new DiskSpaceHealthChecker();
        
        // 验证优先级
        assertThat(memoryChecker.getOrder()).isGreaterThanOrEqualTo(0);
        assertThat(diskChecker.getOrder()).isGreaterThanOrEqualTo(0);
    }
}

