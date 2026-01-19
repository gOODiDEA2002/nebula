package io.nebula.rpc.async.storage.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.rpc.async.execution.AsyncRpcExecution;
import io.nebula.rpc.async.execution.ExecutionStatus;
import io.nebula.rpc.async.storage.AsyncExecutionStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

/**
 * Nacos存储实现（默认）
 * 
 * <p>零配置存储方案，复用已有的Nacos连接
 * 
 * @author Nebula Framework
 * @since 2.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class NacosAsyncExecutionStorage implements AsyncExecutionStorage {
    
    private final ConfigService nacosConfigService;
    private final ObjectMapper objectMapper;
    private final String appName;
    
    private static final long TIMEOUT_MS = 3000;
    
    @Override
    public void save(AsyncRpcExecution execution) {
        try {
            String group = getGroup();
            String dataId = getDataId(execution.getExecutionId());
            String content = objectMapper.writeValueAsString(execution);
            
            boolean success = nacosConfigService.publishConfig(dataId, group, content);
            
            if (!success) {
                throw new RuntimeException("保存到Nacos失败");
            }
            
            log.debug("[AsyncRpc] 保存执行记录到Nacos: executionId={}, group={}, dataId={}", 
                    execution.getExecutionId(), group, dataId);
        } catch (Exception e) {
            log.error("[AsyncRpc] 保存执行记录失败: executionId={}", execution.getExecutionId(), e);
            throw new RuntimeException("保存异步执行记录失败", e);
        }
    }
    
    @Override
    public AsyncRpcExecution findById(String executionId) {
        try {
            String group = getGroup();
            String dataId = getDataId(executionId);
            
            String content = nacosConfigService.getConfig(dataId, group, TIMEOUT_MS);
            
            if (content == null) {
                log.debug("[AsyncRpc] 执行记录不存在: executionId={}", executionId);
                return null;
            }
            
            return objectMapper.readValue(content, AsyncRpcExecution.class);
        } catch (Exception e) {
            log.error("[AsyncRpc] 查询执行记录失败: executionId={}", executionId, e);
            throw new RuntimeException("查询执行记录失败", e);
        }
    }
    
    @Override
    public void updateStatus(String executionId, ExecutionStatus status) {
        AsyncRpcExecution execution = findById(executionId);
        if (execution != null) {
            execution.setStatus(status);
            
            // 根据状态设置时间
            if (status == ExecutionStatus.RUNNING) {
                execution.setStartTime(LocalDateTime.now());
            } else if (status == ExecutionStatus.SUCCESS || 
                       status == ExecutionStatus.FAILED || 
                       status == ExecutionStatus.CANCELLED ||
                       status == ExecutionStatus.TIMEOUT) {
                execution.setFinishTime(LocalDateTime.now());
            }
            
            save(execution);
        }
    }
    
    @Override
    public void updateResult(String executionId, Object result) {
        AsyncRpcExecution execution = findById(executionId);
        if (execution != null) {
            try {
                execution.setResult(objectMapper.writeValueAsString(result));
                execution.setFinishTime(LocalDateTime.now());
                save(execution);
            } catch (Exception e) {
                log.error("[AsyncRpc] 更新结果失败: executionId={}", executionId, e);
                throw new RuntimeException("更新结果失败", e);
            }
        }
    }
    
    @Override
    public void updateError(String executionId, Throwable error) {
        AsyncRpcExecution execution = findById(executionId);
        if (execution != null) {
            execution.setErrorMessage(error.getMessage());
            execution.setErrorStack(getStackTrace(error));
            execution.setFinishTime(LocalDateTime.now());
            save(execution);
        }
    }
    
    private String getGroup() {
        return "ASYNC_RPC_" + appName;
    }
    
    private String getDataId(String executionId) {
        return "execution_" + executionId;
    }
    
    private String getStackTrace(Throwable error) {
        StringWriter sw = new StringWriter();
        error.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
