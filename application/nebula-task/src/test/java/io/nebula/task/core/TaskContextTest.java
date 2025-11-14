package io.nebula.task.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskContext 测试类
 * 测试任务上下文的构建和参数获取功能
 */
@ExtendWith(MockitoExtension.class)
class TaskContextTest {

    @Mock
    private TaskLogger mockLogger;

    /**
     * 测试基本构建功能
     */
    @Test
    void testBuildBasicContext() {
        // 准备测试数据
        String taskId = "task-001";
        String taskName = "test-task";
        String taskAlias = "测试任务";
        TaskType taskType = TaskType.SCHEDULED;
        ExecutionMode executionMode = ExecutionMode.SCHEDULED;
        
        // 构建上下文
        TaskContext context = TaskContext.builder()
                .taskId(taskId)
                .taskName(taskName)
                .taskAlias(taskAlias)
                .taskType(taskType)
                .executionMode(executionMode)
                .logger(mockLogger)
                .logId(1001)
                .logDateTime(System.currentTimeMillis())
                .build();
        
        // 验证结果
        assertThat(context.getTaskId()).isEqualTo(taskId);
        assertThat(context.getTaskName()).isEqualTo(taskName);
        assertThat(context.getTaskAlias()).isEqualTo(taskAlias);
        assertThat(context.getTaskType()).isEqualTo(taskType);
        assertThat(context.getExecutionMode()).isEqualTo(executionMode);
        assertThat(context.getLogger()).isEqualTo(mockLogger);
        assertThat(context.getLogId()).isEqualTo(1001);
        assertThat(context.getStartTime()).isNotNull();
    }

    /**
     * 测试带参数的构建
     */
    @Test
    void testBuildContextWithParameters() {
        // 准备测试参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("param1", "value1");
        parameters.put("param2", 123);
        parameters.put("param3", true);
        
        // 构建上下文
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.MANUAL)
                .executionMode(ExecutionMode.MANUAL)
                .parameters(parameters)
                .logger(mockLogger)
                .build();
        
        // 验证参数
        assertThat(context.getParameters()).isEqualTo(parameters);
        assertThat(context.getParameter("param1")).isEqualTo("value1");
        assertThat(context.getParameter("param2")).isEqualTo(123);
        assertThat(context.getParameter("param3")).isEqualTo(true);
    }

    /**
     * 测试获取字符串参数
     */
    @Test
    void testGetStringParameter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("stringParam", "test-value");
        parameters.put("numberParam", 123);
        
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.MANUAL)
                .parameters(parameters)
                .logger(mockLogger)
                .build();
        
        // 测试获取字符串参数
        assertThat(context.getStringParameter("stringParam")).isEqualTo("test-value");
        assertThat(context.getStringParameter("numberParam")).isEqualTo("123");
        assertThat(context.getStringParameter("notExist")).isNull();
        assertThat(context.getStringParameter("notExist", "default")).isEqualTo("default");
    }

    /**
     * 测试获取整型参数
     */
    @Test
    void testGetIntParameter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("intParam", 100);
        parameters.put("longParam", 200L);
        parameters.put("stringParam", "300");
        parameters.put("invalidParam", "abc");
        
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.MANUAL)
                .parameters(parameters)
                .logger(mockLogger)
                .build();
        
        // 测试获取整型参数
        assertThat(context.getIntParameter("intParam")).isEqualTo(100);
        assertThat(context.getIntParameter("longParam")).isEqualTo(200);
        assertThat(context.getIntParameter("stringParam")).isEqualTo(300);
        assertThat(context.getIntParameter("invalidParam")).isNull();
        assertThat(context.getIntParameter("invalidParam", 999)).isEqualTo(999);
        assertThat(context.getIntParameter("notExist")).isNull();
        assertThat(context.getIntParameter("notExist", 123)).isEqualTo(123);
    }

    /**
     * 测试获取布尔参数
     */
    @Test
    void testGetBooleanParameter() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("boolParam", true);
        parameters.put("stringTrueParam", "true");
        parameters.put("stringFalseParam", "false");
        
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.MANUAL)
                .parameters(parameters)
                .logger(mockLogger)
                .build();
        
        // 测试获取布尔参数
        assertThat(context.getBooleanParameter("boolParam")).isTrue();
        assertThat(context.getBooleanParameter("stringTrueParam")).isTrue();
        assertThat(context.getBooleanParameter("stringFalseParam")).isFalse();
        assertThat(context.getBooleanParameter("notExist")).isNull();
        assertThat(context.getBooleanParameter("notExist", false)).isFalse();
    }

    /**
     * 测试获取参数（带默认值）
     */
    @Test
    void testGetParameterWithDefault() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("existParam", "value");
        
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.MANUAL)
                .parameters(parameters)
                .logger(mockLogger)
                .build();
        
        // 测试获取参数（带默认值）
        assertThat(context.getParameter("existParam", "default")).isEqualTo("value");
        assertThat(context.getParameter("notExist", "default")).isEqualTo("default");
    }

    /**
     * 测试自动设置开始时间
     */
    @Test
    void testAutoSetStartTime() {
        LocalDateTime beforeBuild = LocalDateTime.now();
        
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.MANUAL)
                .logger(mockLogger)
                .build();
        
        LocalDateTime afterBuild = LocalDateTime.now();
        
        // 验证开始时间自动设置
        assertThat(context.getStartTime())
                .isNotNull()
                .isAfterOrEqualTo(beforeBuild)
                .isBeforeOrEqualTo(afterBuild);
    }

    /**
     * 测试手动设置开始时间
     */
    @Test
    void testManualSetStartTime() {
        LocalDateTime customStartTime = LocalDateTime.of(2024, 1, 1, 10, 30);
        
        TaskContext context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-task")
                .taskType(TaskType.MANUAL)
                .startTime(customStartTime)
                .logger(mockLogger)
                .build();
        
        // 验证手动设置的开始时间
        assertThat(context.getStartTime()).isEqualTo(customStartTime);
    }
}

