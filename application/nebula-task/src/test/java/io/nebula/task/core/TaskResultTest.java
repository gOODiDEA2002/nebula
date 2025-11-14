package io.nebula.task.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskResult 测试类
 * 测试任务执行结果的构建和各种静态方法
 */
@ExtendWith(MockitoExtension.class)
class TaskResultTest {

    /**
     * 测试构建基本结果
     */
    @Test
    void testBuildBasicResult() {
        // 构建结果
        TaskResult result = TaskResult.builder()
                .taskId("task-001")
                .taskName("test-task")
                .success(true)
                .message("执行成功")
                .build();
        
        // 验证结果
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTaskName()).isEqualTo("test-task");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("执行成功");
    }

    /**
     * 测试构建成功结果（简单方式）
     */
    @Test
    void testSuccessResult() {
        TaskResult result = TaskResult.success("task-001", "test-task");
        
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTaskName()).isEqualTo("test-task");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("任务执行成功");
    }

    /**
     * 测试构建成功结果（带数据）
     */
    @Test
    void testSuccessResultWithData() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("count", 100);
        data.put("status", "completed");
        
        TaskResult result = TaskResult.success("task-001", "test-task", "处理完成", data);
        
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTaskName()).isEqualTo("test-task");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("处理完成");
        assertThat(result.getData()).isEqualTo(data);
    }

    /**
     * 测试构建失败结果
     */
    @Test
    void testFailureResult() {
        TaskResult result = TaskResult.failure("task-001", "test-task", "执行失败");
        
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTaskName()).isEqualTo("test-task");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("执行失败");
    }

    /**
     * 测试构建失败结果（带异常）
     */
    @Test
    void testFailureResultWithException() {
        Exception exception = new RuntimeException("测试异常");
        
        TaskResult result = TaskResult.failure("task-001", "test-task", exception);
        
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTaskName()).isEqualTo("test-task");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("测试异常");
        assertThat(result.getException()).isEqualTo(exception);
    }

    /**
     * 测试基于上下文创建成功结果
     */
    @Test
    void testSuccessResultFromContext() {
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.SCHEDULED)
                .build();
        
        TaskResult result = TaskResult.success(context);
        
        assertThat(result.getTaskId()).isEqualTo(context.getTaskId());
        assertThat(result.getTaskName()).isEqualTo(context.getTaskName());
        assertThat(result.isSuccess()).isTrue();
    }

    /**
     * 测试基于上下文创建成功结果（带数据）
     */
    @Test
    void testSuccessResultFromContextWithData() {
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.SCHEDULED)
                .build();
        
        HashMap<String, Object> data = new HashMap<>();
        data.put("result", "ok");
        
        TaskResult result = TaskResult.success(context, "处理完成", data);
        
        assertThat(result.getTaskId()).isEqualTo(context.getTaskId());
        assertThat(result.getTaskName()).isEqualTo(context.getTaskName());
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMessage()).isEqualTo("处理完成");
        assertThat(result.getData()).isEqualTo(data);
    }

    /**
     * 测试基于上下文创建失败结果
     */
    @Test
    void testFailureResultFromContext() {
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.SCHEDULED)
                .build();
        
        TaskResult result = TaskResult.failure(context, "执行失败");
        
        assertThat(result.getTaskId()).isEqualTo(context.getTaskId());
        assertThat(result.getTaskName()).isEqualTo(context.getTaskName());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("执行失败");
    }

    /**
     * 测试基于上下文创建失败结果（带异常）
     */
    @Test
    void testFailureResultFromContextWithException() {
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.SCHEDULED)
                .build();
        
        Exception exception = new RuntimeException("测试异常");
        
        TaskResult result = TaskResult.failure(context, exception);
        
        assertThat(result.getTaskId()).isEqualTo(context.getTaskId());
        assertThat(result.getTaskName()).isEqualTo(context.getTaskName());
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("测试异常");
        assertThat(result.getException()).isEqualTo(exception);
    }

    /**
     * 测试自动计算执行时间
     */
    @Test
    void testAutoCalculateDuration() {
        LocalDateTime startTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 1, 1, 10, 0, 5);
        
        TaskResult result = TaskResult.builder()
                .taskId("task-001")
                .taskName("test-task")
                .success(true)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        
        // 验证自动计算的执行时间（5秒 = 5000毫秒）
        assertThat(result.getDuration()).isEqualTo(5000L);
    }

    /**
     * 测试手动设置执行时间
     */
    @Test
    void testManualSetDuration() {
        TaskResult result = TaskResult.builder()
                .taskId("task-001")
                .taskName("test-task")
                .success(true)
                .duration(3000L)
                .build();
        
        // 验证手动设置的执行时间
        assertThat(result.getDuration()).isEqualTo(3000L);
    }

    /**
     * 测试 toString 方法
     */
    @Test
    void testToString() {
        TaskResult result = TaskResult.builder()
                .taskId("task-001")
                .taskName("test-task")
                .success(true)
                .message("执行成功")
                .duration(1000L)
                .build();
        
        String resultString = result.toString();
        
        assertThat(resultString)
                .contains("task-001")
                .contains("test-task")
                .contains("true")
                .contains("执行成功")
                .contains("1000");
    }

    /**
     * 测试带完整信息的结果
     */
    @Test
    void testCompleteResult() {
        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusSeconds(2);
        Exception exception = new RuntimeException("测试异常");
        HashMap<String, Object> data = new HashMap<>();
        data.put("processed", 100);
        
        TaskResult result = TaskResult.builder()
                .taskId("task-001")
                .taskName("test-task")
                .success(false)
                .message("部分失败")
                .data(data)
                .startTime(startTime)
                .endTime(endTime)
                .exception(exception)
                .build();
        
        // 验证所有字段
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTaskName()).isEqualTo("test-task");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("部分失败");
        assertThat(result.getData()).isEqualTo(data);
        assertThat(result.getStartTime()).isEqualTo(startTime);
        assertThat(result.getEndTime()).isEqualTo(endTime);
        assertThat(result.getException()).isEqualTo(exception);
        assertThat(result.getDuration()).isEqualTo(2000L);
    }
}

