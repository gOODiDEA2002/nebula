package io.nebula.example.rpc.async.client.controller;

import io.nebula.example.rpc.async.api.dto.ProcessRequest;
import io.nebula.example.rpc.async.api.dto.ProcessResult;
import io.nebula.example.rpc.async.api.dto.ProcessType;
import io.nebula.example.rpc.async.client.controller.vo.AsyncTaskResponse;
import io.nebula.example.rpc.async.client.controller.vo.BatchTaskRequest;
import io.nebula.example.rpc.async.client.controller.vo.ExecutionStatusResponse;
import io.nebula.example.rpc.async.client.service.TaskService;
import io.nebula.rpc.async.execution.AsyncRpcExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 任务控制器
 * 
 * <p>提供异步任务提交、查询、取消等REST API。
 * 
 * @author Nebula Framework
 */
@Slf4j
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {
    
    private final TaskService taskService;
    
    /**
     * 提交异步任务
     * 
     * POST /api/tasks/async
     * 
     * @param request 处理请求
     * @return 异步任务响应（包含executionId）
     */
    @PostMapping("/async")
    public ResponseEntity<AsyncTaskResponse> submitAsyncTask(@RequestBody ProcessRequest request) {
        // 如果没有指定taskId，自动生成
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            request.setTaskId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        
        log.info("收到异步任务请求: taskId={}, type={}", request.getTaskId(), request.getType());
        
        String executionId = taskService.submitAsyncTask(request);
        
        AsyncTaskResponse response = AsyncTaskResponse.builder()
                .executionId(executionId)
                .taskId(request.getTaskId())
                .status("PENDING")
                .message("任务已提交，正在后台处理")
                .queryUrl("/api/tasks/status/" + executionId)
                .resultUrl("/api/tasks/result/" + executionId)
                .build();
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * 提交批量异步任务
     * 
     * POST /api/tasks/batch
     * 
     * @param batchRequest 批量请求
     * @return 异步任务响应
     */
    @PostMapping("/batch")
    public ResponseEntity<AsyncTaskResponse> submitBatchTask(@RequestBody BatchTaskRequest batchRequest) {
        List<ProcessRequest> requests = batchRequest.getRequests();
        
        // 为没有taskId的请求生成ID
        for (int i = 0; i < requests.size(); i++) {
            ProcessRequest req = requests.get(i);
            if (req.getTaskId() == null || req.getTaskId().isEmpty()) {
                req.setTaskId("batch-" + i + "-" + System.currentTimeMillis());
            }
        }
        
        log.info("收到批量任务请求: 共 {} 个任务", requests.size());
        
        String executionId = taskService.submitBatchAsyncTask(requests);
        
        AsyncTaskResponse response = AsyncTaskResponse.builder()
                .executionId(executionId)
                .taskId("batch-" + executionId.substring(0, 8))
                .status("PENDING")
                .message(String.format("批量任务已提交，共 %d 个任务正在处理", requests.size()))
                .queryUrl("/api/tasks/status/" + executionId)
                .resultUrl("/api/tasks/result/batch/" + executionId)
                .build();
        
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * 查询执行状态
     * 
     * GET /api/tasks/status/{executionId}
     * 
     * @param executionId 执行ID
     * @return 执行状态
     */
    @GetMapping("/status/{executionId}")
    public ResponseEntity<ExecutionStatusResponse> getExecutionStatus(
            @PathVariable String executionId) {
        
        log.debug("查询执行状态: executionId={}", executionId);
        
        AsyncRpcExecution execution = taskService.getExecution(executionId);
        
        if (execution == null) {
            return ResponseEntity.notFound().build();
        }
        
        ExecutionStatusResponse response = ExecutionStatusResponse.builder()
                .executionId(execution.getExecutionId())
                .status(execution.getStatus().name())
                .interfaceName(execution.getInterfaceName())
                .methodName(execution.getMethodName())
                .createTime(execution.getCreateTime() != null 
                        ? execution.getCreateTime().toString() : null)
                .startTime(execution.getStartTime() != null 
                        ? execution.getStartTime().toString() : null)
                .finishTime(execution.getFinishTime() != null 
                        ? execution.getFinishTime().toString() : null)
                .errorMessage(execution.getErrorMessage())
                .build();
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 获取执行结果
     * 
     * GET /api/tasks/result/{executionId}
     * 
     * @param executionId 执行ID
     * @return 处理结果
     */
    @GetMapping("/result/{executionId}")
    public ResponseEntity<ProcessResult> getResult(@PathVariable String executionId) {
        log.debug("获取执行结果: executionId={}", executionId);
        
        ProcessResult result = taskService.getResult(executionId);
        
        if (result == null) {
            // 查询状态判断是未完成还是不存在
            AsyncRpcExecution execution = taskService.getExecution(executionId);
            if (execution == null) {
                return ResponseEntity.notFound().build();
            }
            // 任务未完成
            return ResponseEntity.accepted().build();
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取批量执行结果
     * 
     * GET /api/tasks/result/batch/{executionId}
     * 
     * @param executionId 执行ID
     * @return 处理结果列表
     */
    @GetMapping("/result/batch/{executionId}")
    public ResponseEntity<List<ProcessResult>> getBatchResult(@PathVariable String executionId) {
        log.debug("获取批量执行结果: executionId={}", executionId);
        
        List<ProcessResult> results = taskService.getBatchResult(executionId);
        
        if (results == null) {
            AsyncRpcExecution execution = taskService.getExecution(executionId);
            if (execution == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.accepted().build();
        }
        
        return ResponseEntity.ok(results);
    }
    
    /**
     * 取消执行
     * 
     * DELETE /api/tasks/{executionId}
     * 
     * @param executionId 执行ID
     * @return 取消结果
     */
    @DeleteMapping("/{executionId}")
    public ResponseEntity<Map<String, Object>> cancelExecution(@PathVariable String executionId) {
        log.info("取消执行: executionId={}", executionId);
        
        boolean cancelled = taskService.cancelExecution(executionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("executionId", executionId);
        response.put("cancelled", cancelled);
        response.put("message", cancelled ? "任务已取消" : "无法取消任务（可能已完成或正在执行）");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 同步调用（用于对比测试）
     * 
     * POST /api/tasks/sync
     * 
     * @param request 处理请求
     * @return 处理结果
     */
    @PostMapping("/sync")
    public ResponseEntity<ProcessResult> processSyncTask(@RequestBody ProcessRequest request) {
        if (request.getTaskId() == null || request.getTaskId().isEmpty()) {
            request.setTaskId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
        
        log.info("收到同步任务请求: taskId={}, type={}", request.getTaskId(), request.getType());
        
        ProcessResult result = taskService.processSyncTask(request);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 快速测试接口：提交一个简单的异步任务
     * 
     * GET /api/tasks/test?delay=5&type=DATA_IMPORT
     */
    @GetMapping("/test")
    public ResponseEntity<AsyncTaskResponse> testAsyncTask(
            @RequestParam(defaultValue = "5") int delay,
            @RequestParam(defaultValue = "DATA_IMPORT") ProcessType type) {
        
        ProcessRequest request = ProcessRequest.builder()
                .taskId("test-" + System.currentTimeMillis())
                .dataSource("test-datasource")
                .type(type)
                .delaySeconds(delay)
                .remark("测试任务")
                .params(Map.of("testParam", "testValue"))
                .build();
        
        return submitAsyncTask(request);
    }
}
