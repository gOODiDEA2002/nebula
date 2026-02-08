package io.nebula.example.rpc.async.client.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.nebula.example.rpc.async.api.DataProcessRpcClient;
import io.nebula.example.rpc.async.api.dto.ProcessRequest;
import io.nebula.example.rpc.async.api.dto.ProcessResult;
import io.nebula.rpc.async.execution.AsyncRpcExecution;
import io.nebula.rpc.async.execution.AsyncRpcExecutionManager;
import io.nebula.rpc.async.execution.AsyncRpcResult;
import io.nebula.rpc.async.execution.ExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 任务服务
 * 
 * <p>封装异步RPC调用和状态查询逻辑。
 * 
 * @author Nebula Framework
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final DataProcessRpcClient dataProcessRpcClient;
    private final AsyncRpcExecutionManager executionManager;
    private final ObjectMapper objectMapper;
    
    /**
     * 提交异步任务
     * 
     * @param request 处理请求
     * @return 执行ID
     */
    public String submitAsyncTask(ProcessRequest request) {
        log.info("提交异步任务: taskId={}, type={}", request.getTaskId(), request.getType());
        
        // 调用异步方法
        AsyncRpcResult<ProcessResult> result = dataProcessRpcClient.processDataAsync(request);
        
        String executionId = result.getExecutionId();
        log.info("异步任务已提交: executionId={}", executionId);
        
        return executionId;
    }
    
    /**
     * 提交批量异步任务
     * 
     * @param requests 请求列表
     * @return 执行ID
     */
    public String submitBatchAsyncTask(List<ProcessRequest> requests) {
        log.info("提交批量异步任务: 共 {} 个任务", requests.size());
        
        AsyncRpcResult<List<ProcessResult>> result = 
                dataProcessRpcClient.batchProcessAsync(requests);
        
        String executionId = result.getExecutionId();
        log.info("批量异步任务已提交: executionId={}", executionId);
        
        return executionId;
    }
    
    /**
     * 查询执行状态
     * 
     * @param executionId 执行ID
     * @return 执行记录
     */
    public AsyncRpcExecution getExecution(String executionId) {
        log.debug("查询执行状态: executionId={}", executionId);
        return executionManager.getExecution(executionId);
    }
    
    /**
     * 获取执行结果
     * 
     * @param executionId 执行ID
     * @return 处理结果，如果未完成返回null
     */
    public ProcessResult getResult(String executionId) {
        AsyncRpcExecution execution = executionManager.getExecution(executionId);
        
        if (execution == null) {
            log.warn("执行记录不存在: executionId={}", executionId);
            return null;
        }
        
        if (execution.getStatus() != ExecutionStatus.SUCCESS) {
            log.debug("任务未完成: executionId={}, status={}", 
                    executionId, execution.getStatus());
            return null;
        }
        
        // 反序列化结果
        try {
            String resultJson = execution.getResult();
            ProcessResult result = objectMapper.readValue(resultJson, ProcessResult.class);
            log.info("获取执行结果成功: executionId={}, taskId={}", 
                    executionId, result.getTaskId());
            return result;
        } catch (Exception e) {
            log.error("反序列化结果失败: executionId={}", executionId, e);
            return null;
        }
    }
    
    /**
     * 获取批量执行结果
     * 
     * @param executionId 执行ID
     * @return 处理结果列表，如果未完成返回null
     */
    @SuppressWarnings("unchecked")
    public List<ProcessResult> getBatchResult(String executionId) {
        AsyncRpcExecution execution = executionManager.getExecution(executionId);
        
        if (execution == null || execution.getStatus() != ExecutionStatus.SUCCESS) {
            return null;
        }
        
        try {
            String resultJson = execution.getResult();
            return objectMapper.readValue(resultJson, 
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, ProcessResult.class));
        } catch (Exception e) {
            log.error("反序列化批量结果失败: executionId={}", executionId, e);
            return null;
        }
    }
    
    /**
     * 取消执行
     * 
     * @param executionId 执行ID
     * @return 是否成功取消
     */
    public boolean cancelExecution(String executionId) {
        log.info("取消执行: executionId={}", executionId);
        return executionManager.cancel(executionId);
    }
    
    /**
     * 同步调用（用于对比测试）
     * 
     * @param request 处理请求
     * @return 处理结果
     */
    public ProcessResult processSyncTask(ProcessRequest request) {
        log.info("同步处理任务: taskId={}, type={}", request.getTaskId(), request.getType());
        return dataProcessRpcClient.processData(request);
    }
}
