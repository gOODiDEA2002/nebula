package io.nebula.task.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * TaskExecutor 测试类
 * 测试任务执行器接口的实现
 */
@ExtendWith(MockitoExtension.class)
class TaskExecutorTest {

    @Mock
    private TaskLogger mockLogger;

    private TestTaskExecutor executor;
    private TaskContext context;

    @BeforeEach
    void setUp() {
        executor = new TestTaskExecutor();
        context = TaskContext.builder()
                .taskId("task-001")
                .taskName("test-executor")
                .taskType(TaskType.MANUAL)
                .executionMode(ExecutionMode.MANUAL)
                .parameters(new HashMap<>())
                .logger(mockLogger)
                .build();
    }

    /**
     * 测试执行器名称
     */
    @Test
    void testGetExecutorName() {
        assertThat(executor.getExecutorName()).isEqualTo("test-executor");
    }

    /**
     * 测试执行器描述
     */
    @Test
    void testGetDescription() {
        assertThat(executor.getDescription()).isEqualTo("任务执行器: test-executor");
    }

    /**
     * 测试执行任务
     */
    @Test
    void testExecute() {
        TaskResult result = executor.execute(context);
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getTaskName()).isEqualTo("test-executor");
    }

    /**
     * 测试支持任务类型
     */
    @Test
    void testSupportsTaskType() {
        assertThat(executor.supports(TaskType.MANUAL)).isTrue();
        assertThat(executor.supports(TaskType.SCHEDULED)).isTrue();
    }

    /**
     * 测试支持任务名称
     */
    @Test
    void testSupportsTaskName() {
        assertThat(executor.supports("test-executor")).isTrue();
        assertThat(executor.supports("other-executor")).isFalse();
    }

    /**
     * 测试执行前的初始化
     */
    @Test
    void testBeforeExecute() {
        CallbackTrackingExecutor trackingExecutor = new CallbackTrackingExecutor();
        
        trackingExecutor.beforeExecute(context);
        
        assertThat(trackingExecutor.beforeExecuteCalled).isTrue();
    }

    /**
     * 测试执行后的清理
     */
    @Test
    void testAfterExecute() {
        CallbackTrackingExecutor trackingExecutor = new CallbackTrackingExecutor();
        TaskResult result = TaskResult.success(context);
        
        trackingExecutor.afterExecute(context, result);
        
        assertThat(trackingExecutor.afterExecuteCalled).isTrue();
    }

    /**
     * 测试异常处理
     */
    @Test
    void testHandleException() {
        Exception exception = new RuntimeException("测试异常");
        
        TaskResult result = executor.handleException(context, exception);
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getTaskId()).isEqualTo("task-001");
        assertThat(result.getException()).isEqualTo(exception);
    }

    /**
     * 测试自定义异常处理
     */
    @Test
    void testCustomExceptionHandling() {
        CustomExceptionHandlerExecutor customExecutor = new CustomExceptionHandlerExecutor();
        Exception exception = new RuntimeException("测试异常");
        
        TaskResult result = customExecutor.handleException(context, exception);
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("自定义异常处理");
    }

    /**
     * 测试执行器 - 用于测试
     */
    static class TestTaskExecutor implements TaskExecutor {
        
        @Override
        public TaskResult execute(TaskContext context) {
            return TaskResult.success(context);
        }

        @Override
        public String getExecutorName() {
            return "test-executor";
        }
    }

    /**
     * 回调追踪执行器 - 用于测试生命周期回调
     */
    static class CallbackTrackingExecutor implements TaskExecutor {
        
        boolean beforeExecuteCalled = false;
        boolean afterExecuteCalled = false;

        @Override
        public TaskResult execute(TaskContext context) {
            return TaskResult.success(context);
        }

        @Override
        public String getExecutorName() {
            return "callback-tracking-executor";
        }

        @Override
        public void beforeExecute(TaskContext context) {
            beforeExecuteCalled = true;
        }

        @Override
        public void afterExecute(TaskContext context, TaskResult result) {
            afterExecuteCalled = true;
        }
    }

    /**
     * 自定义异常处理执行器 - 用于测试自定义异常处理
     */
    static class CustomExceptionHandlerExecutor implements TaskExecutor {
        
        @Override
        public TaskResult execute(TaskContext context) {
            return TaskResult.success(context);
        }

        @Override
        public String getExecutorName() {
            return "custom-exception-handler";
        }

        @Override
        public TaskResult handleException(TaskContext context, Exception exception) {
            return TaskResult.failure(context, "自定义异常处理");
        }
    }
}

