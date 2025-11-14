package io.nebula.task.execution;

import io.nebula.task.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TaskEngine单元测试
 */
@ExtendWith(MockitoExtension.class)
class TaskEngineTest {
    
    @Mock
    private TaskRegistry taskRegistry;
    
    @InjectMocks
    private TaskEngine taskEngine;
    
    @BeforeEach
    void setUp() {
        // TaskEngine 使用 @Autowired 注入 TaskRegistry
    }
    
    /**
     * 测试注册任务执行器
     */
    @Test
    void testRegisterExecutor() {
        String taskName = "test-task";
        TaskExecutor executor = new TestTaskExecutor(taskName);
        
        taskRegistry.registerExecutor(executor);
        
        verify(taskRegistry, times(1)).registerExecutor(executor);
    }
    
    /**
     * 测试查找执行器
     */
    @Test
    void testFindExecutor() {
        String taskName = "test-task";
        TaskExecutor executor = new TestTaskExecutor(taskName);
        when(taskRegistry.findExecutor(taskName)).thenReturn(executor);
        
        TaskExecutor result = taskRegistry.findExecutor(taskName);
        
        assertThat(result).isNotNull();
        assertThat(result.getExecutorName()).isEqualTo(taskName);
    }
    
    /**
     * 测试同步执行任务
     */
    @Test
    void testExecuteSync() {
        String taskName = "test-task";
        TaskExecutor executor = new TestTaskExecutor(taskName);
        Map<String, Object> parameters = new HashMap<>();
        
        when(taskRegistry.findExecutor(taskName)).thenReturn(executor);
        
        TaskResult result = taskEngine.executeSync(taskName, parameters, 1001, System.currentTimeMillis());
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getTaskName()).isEqualTo(taskName);
    }
    
    /**
     * 测试同步执行任务（找不到执行器）
     */
    @Test
    void testExecuteSyncExecutorNotFound() {
        String taskName = "non-exist-task";
        Map<String, Object> parameters = new HashMap<>();
        
        when(taskRegistry.findExecutor(taskName)).thenReturn(null);
        
        TaskResult result = taskEngine.executeSync(taskName, parameters, 1001, System.currentTimeMillis());
        
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("找不到任务执行器");
    }
    
    /**
     * 测试异步执行任务
     */
    @Test
    void testExecuteAsync() throws InterruptedException {
        String taskName = "test-task";
        TaskExecutor executor = new TestTaskExecutor(taskName);
        Map<String, Object> parameters = new HashMap<>();
        
        when(taskRegistry.findExecutor(taskName)).thenReturn(executor);
        
        taskEngine.executeAsync(taskName, parameters, 1001, System.currentTimeMillis());
        
        // 等待异步任务完成
        Thread.sleep(100);
        
        // 验证任务被查找
        verify(taskRegistry, atLeastOnce()).findExecutor(taskName);
    }
    
    /**
     * 测试检查任务是否运行中
     */
    @Test
    void testIsTaskRunning() throws InterruptedException {
        String taskName = "long-running-task";
        TaskExecutor executor = new LongRunningTaskExecutor(taskName);
        Map<String, Object> parameters = new HashMap<>();
        
        when(taskRegistry.findExecutor(taskName)).thenReturn(executor);
        
        // 异步执行任务
        taskEngine.executeAsync(taskName, parameters, 1001, System.currentTimeMillis());
        
        // 稍等一下，让任务开始执行
        Thread.sleep(50);
        
        // 检查任务是否运行中
        boolean isRunning = taskEngine.isTaskRunning(taskName);
        assertThat(isRunning).isTrue();
        
        // 等待任务完成
        Thread.sleep(200);
        
        // 再次检查，任务应该已经完成
        isRunning = taskEngine.isTaskRunning(taskName);
        assertThat(isRunning).isFalse();
    }
    
    /**
     * 测试获取任务日志
     */
    @Test
    void testGetTaskLog() {
        String taskName = "test-task";
        TaskExecutor executor = new TestTaskExecutor(taskName);
        Map<String, Object> parameters = new HashMap<>();
        int logId = 1001;
        
        when(taskRegistry.findExecutor(taskName)).thenReturn(executor);
        
        taskEngine.executeSync(taskName, parameters, logId, System.currentTimeMillis());
        
        String logContent = taskEngine.getTaskLog(logId);
        assertThat(logContent).isNotNull();
        
        int logLines = taskEngine.getTaskLogLines(logId);
        assertThat(logLines).isGreaterThanOrEqualTo(0);
    }
    
    /**
     * 测试任务执行器 - 用于测试
     */
    static class TestTaskExecutor implements TaskExecutor {
        
        private final String executorName;
        
        TestTaskExecutor(String executorName) {
            this.executorName = executorName;
        }
        
        @Override
        public TaskResult execute(TaskContext context) {
            return TaskResult.success(context);
        }

        @Override
        public String getExecutorName() {
            return executorName;
        }
    }
    
    /**
     * 长时间运行的任务执行器 - 用于测试异步任务
     */
    static class LongRunningTaskExecutor implements TaskExecutor {
        
        private final String executorName;
        
        LongRunningTaskExecutor(String executorName) {
            this.executorName = executorName;
        }
        
        @Override
        public TaskResult execute(TaskContext context) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return TaskResult.success(context);
        }

        @Override
        public String getExecutorName() {
            return executorName;
        }
    }
}

