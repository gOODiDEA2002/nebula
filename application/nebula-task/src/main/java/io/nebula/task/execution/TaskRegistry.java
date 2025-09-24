package io.nebula.task.execution;

import io.nebula.task.core.TaskExecutor;
import io.nebula.task.core.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务注册器
 * 管理所有任务执行器的注册和查找
 */
@Component
public class TaskRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(TaskRegistry.class);
    
    /**
     * 按名称索引的执行器
     */
    private final Map<String, TaskExecutor> executorsByName = new ConcurrentHashMap<>();
    
    /**
     * 按类型索引的执行器
     */
    private final Map<TaskType, Map<String, TaskExecutor>> executorsByType = new ConcurrentHashMap<>();
    
    /**
     * 注册任务执行器
     * 
     * @param executor 任务执行器
     */
    public void registerExecutor(TaskExecutor executor) {
        String name = executor.getExecutorName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("任务执行器名称不能为空: " + executor.getClass().getName());
        }
        
        if (executorsByName.containsKey(name)) {
            logger.warn("任务执行器名称重复，将覆盖原有执行器: {}", name);
        }
        
        // 按名称注册
        executorsByName.put(name, executor);
        
        // 按类型注册
        for (TaskType type : TaskType.values()) {
            if (executor.supports(type)) {
                executorsByType.computeIfAbsent(type, k -> new ConcurrentHashMap<>())
                        .put(name, executor);
            }
        }
        
        logger.info("注册任务执行器: {} [{}]", name, executor.getClass().getSimpleName());
    }
    
    /**
     * 根据名称查找执行器
     * 
     * @param name 执行器名称
     * @return 执行器，如果不存在返回null
     */
    public TaskExecutor findExecutor(String name) {
        return executorsByName.get(name);
    }
    
    /**
     * 根据类型查找执行器
     * 
     * @param type 任务类型
     * @return 执行器集合
     */
    public Collection<TaskExecutor> findExecutors(TaskType type) {
        Map<String, TaskExecutor> executors = executorsByType.get(type);
        return executors != null ? executors.values() : java.util.Collections.emptyList();
    }
    
    /**
     * 根据类型和名称查找执行器
     * 
     * @param type 任务类型
     * @param name 执行器名称
     * @return 执行器，如果不存在返回null
     */
    public TaskExecutor findExecutor(TaskType type, String name) {
        Map<String, TaskExecutor> executors = executorsByType.get(type);
        return executors != null ? executors.get(name) : null;
    }
    
    /**
     * 获取所有注册的执行器
     * 
     * @return 所有执行器
     */
    public Collection<TaskExecutor> getAllExecutors() {
        return executorsByName.values();
    }
    
    /**
     * 获取注册的执行器数量
     * 
     * @return 执行器数量
     */
    public int getExecutorCount() {
        return executorsByName.size();
    }
    
    /**
     * 检查是否存在指定名称的执行器
     * 
     * @param name 执行器名称
     * @return 是否存在
     */
    public boolean hasExecutor(String name) {
        return executorsByName.containsKey(name);
    }
    
    /**
     * 注销执行器
     * 
     * @param name 执行器名称
     */
    public void unregisterExecutor(String name) {
        TaskExecutor executor = executorsByName.remove(name);
        if (executor != null) {
            // 从类型索引中移除
            for (Map<String, TaskExecutor> typeExecutors : executorsByType.values()) {
                typeExecutors.remove(name);
            }
            logger.info("注销任务执行器: {}", name);
        }
    }
    
    /**
     * 清空所有执行器
     */
    public void clear() {
        executorsByName.clear();
        executorsByType.clear();
        logger.info("清空所有任务执行器");
    }
}
