package io.nebula.task.execution;

import io.nebula.task.core.*;
import io.nebula.core.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 任务执行引擎
 * 负责任务的执行、监控和管理
 */
@Component
public class TaskEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskEngine.class);
    
    @Autowired
    private TaskRegistry taskRegistry;
    
    /**
     * 线程池执行器
     */
    private final ExecutorService threadPool = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("nebula-task-" + thread.getId());
        thread.setDaemon(true);
        return thread;
    });
    
    /**
     * 正在运行的任务
     */
    private final Map<String, TaskExecutionInfo> runningTasks = new ConcurrentHashMap<>();
    
    /**
     * 日志存储 - 临时存储任务日志
     */
    private final Map<Integer, DefaultTaskLogger> logStorage = new ConcurrentHashMap<>();
    
    /**
     * 同步执行任务
     * 
     * @param taskName 任务名称
     * @param parameters 任务参数
     * @param logId 日志ID
     * @param logDateTime 日志时间
     * @return 执行结果
     */
    public TaskResult executeSync(String taskName, Map<String, Object> parameters, 
                                 int logId, long logDateTime) {
        
        TaskExecutor executor = taskRegistry.findExecutor(taskName);
        if (executor == null) {
            String errorMsg = "找不到任务执行器: " + taskName;
            logger.error(errorMsg);
            return TaskResult.failure(taskName, taskName, errorMsg);
        }
        
        // 创建执行上下文
        DefaultTaskLogger taskLogger = new DefaultTaskLogger(logId, logDateTime);
        logStorage.put(logId, taskLogger);
        
        TaskContext context = TaskContext.builder()
                .taskId(String.valueOf(logId))
                .taskName(taskName)
                .taskType(TaskType.MANUAL)
                .executionMode(ExecutionMode.MANUAL)
                .parameters(parameters)
                .logger(taskLogger)
                .logId(logId)
                .logDateTime(logDateTime)
                .build();
        
        return executeTask(executor, context);
    }
    
    /**
     * 异步执行任务
     * 
     * @param taskName 任务名称
     * @param parameters 任务参数
     * @param logId 日志ID
     * @param logDateTime 日志时间
     */
    public void executeAsync(String taskName, Map<String, Object> parameters, 
                            int logId, long logDateTime) {
        
        TaskExecutor executor = taskRegistry.findExecutor(taskName);
        if (executor == null) {
            logger.error("找不到任务执行器: {}", taskName);
            return;
        }
        
        // 检查是否已在运行
        String taskKey = taskName;
        if (runningTasks.containsKey(taskKey)) {
            logger.warn("任务 {} 正在执行，跳过此次调度", taskName);
            return;
        }
        
        // 创建执行上下文
        DefaultTaskLogger taskLogger = new DefaultTaskLogger(logId, logDateTime);
        logStorage.put(logId, taskLogger);
        
        TaskContext context = TaskContext.builder()
                .taskId(String.valueOf(logId))
                .taskName(taskName)
                .taskType(TaskType.SCHEDULED)
                .executionMode(ExecutionMode.SCHEDULED)
                .parameters(parameters)
                .logger(taskLogger)
                .logId(logId)
                .logDateTime(logDateTime)
                .build();
        
        // 异步执行
        threadPool.execute(() -> {
            try {
                // 标记任务开始
                TaskExecutionInfo executionInfo = new TaskExecutionInfo(taskName, LocalDateTime.now());
                runningTasks.put(taskKey, executionInfo);
                
                executeTask(executor, context);
                
            } finally {
                // 移除运行标记
                runningTasks.remove(taskKey);
            }
        });
    }
    
    /**
     * 执行任务的核心逻辑
     * 
     * @param executor 任务执行器
     * @param context 任务上下文
     * @return 执行结果
     */
    private TaskResult executeTask(TaskExecutor executor, TaskContext context) {
        LocalDateTime startTime = LocalDateTime.now();
        TaskResult result = null;
        
        try {
            logger.info("开始执行任务: {} [{}]", context.getTaskName(), context.getExecutionMode());
            context.getLogger().info("任务开始执行: %s", context.getTaskName());
            
            // 执行前置操作
            executor.beforeExecute(context);
            
            // 执行任务
            result = executor.execute(context);
            
            if (result == null) {
                result = TaskResult.success(context);
            }
            
            // 更新执行时间
            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).toMillis();
            
            result = TaskResult.builder()
                    .taskId(result.getTaskId())
                    .taskName(result.getTaskName())
                    .success(result.isSuccess())
                    .message(result.getMessage())
                    .data(result.getData())
                    .startTime(startTime)
                    .endTime(endTime)
                    .duration(duration)
                    .exception(result.getException())
                    .build();
            
            // 执行后置操作
            executor.afterExecute(context, result);
            
            if (result.isSuccess()) {
                logger.info("任务执行成功: {} - {} [耗时: {}ms]", 
                    context.getTaskName(), result.getMessage(), duration);
                context.getLogger().info("任务执行成功，耗时: %dms", duration);
            } else {
                logger.warn("任务执行失败: {} - {}", context.getTaskName(), result.getMessage());
                context.getLogger().error("任务执行失败: %s", result.getMessage());
            }
            
        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).toMillis();
            
            logger.error("任务执行异常: {} [耗时: {}ms]", context.getTaskName(), duration, e);
            context.getLogger().error("任务执行异常: %s", e.getMessage(), e);
            
            // 调用异常处理
            result = executor.handleException(context, e);
            if (result == null) {
                result = TaskResult.failure(context, e);
            }
            
            // 更新时间信息
            result = TaskResult.builder()
                    .taskId(result.getTaskId())
                    .taskName(result.getTaskName())
                    .success(false)
                    .message(result.getMessage())
                    .data(result.getData())
                    .startTime(startTime)
                    .endTime(endTime)
                    .duration(duration)
                    .exception(e)
                    .build();
        }
        
        return result;
    }
    
    /**
     * 获取任务日志
     * 
     * @param logId 日志ID
     * @return 日志内容
     */
    public String getTaskLog(int logId) {
        DefaultTaskLogger taskLogger = logStorage.get(logId);
        return taskLogger != null ? taskLogger.getLogContent() : "";
    }
    
    /**
     * 获取任务日志行数
     * 
     * @param logId 日志ID
     * @return 日志行数
     */
    public int getTaskLogLines(int logId) {
        DefaultTaskLogger taskLogger = logStorage.get(logId);
        return taskLogger != null ? taskLogger.getLogLines() : 0;
    }
    
    /**
     * 清理过期日志
     * 
     * @param logId 日志ID
     */
    public void cleanupLog(int logId) {
        logStorage.remove(logId);
    }
    
    /**
     * 检查任务是否正在运行
     * 
     * @param taskName 任务名称
     * @return 是否正在运行
     */
    public boolean isTaskRunning(String taskName) {
        return runningTasks.containsKey(taskName);
    }
    
    /**
     * 获取正在运行的任务数量
     * 
     * @return 运行中任务数量
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }
    
    /**
     * 关闭执行引擎
     */
    public void shutdown() {
        threadPool.shutdown();
        logger.info("任务执行引擎已关闭");
    }
    
    /**
     * 任务执行信息
     */
    private static class TaskExecutionInfo {
        private final String taskName;
        private final LocalDateTime startTime;
        
        public TaskExecutionInfo(String taskName, LocalDateTime startTime) {
            this.taskName = taskName;
            this.startTime = startTime;
        }
        
        public String getTaskName() { return taskName; }
        public LocalDateTime getStartTime() { return startTime; }
    }
}
