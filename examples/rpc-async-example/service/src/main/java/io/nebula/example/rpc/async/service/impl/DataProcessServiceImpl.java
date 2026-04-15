package io.nebula.example.rpc.async.service.impl;

import io.nebula.example.rpc.async.api.DataProcessRpcClient;
import io.nebula.example.rpc.async.api.DataProcessService;
import io.nebula.example.rpc.async.api.dto.ProcessRequest;
import io.nebula.example.rpc.async.api.dto.ProcessResult;
import io.nebula.example.rpc.async.api.dto.ProcessType;
import io.nebula.rpc.core.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 数据处理服务实现
 * 
 * <p>实现 {@link DataProcessService} 接口，提供实际的业务处理逻辑。
 * 注意：所有方法都是同步的，异步包装由客户端框架处理。
 * 
 * @author Nebula Framework
 */
@Slf4j
@RpcService(DataProcessRpcClient.class)
public class DataProcessServiceImpl implements DataProcessService {
    
    /**
     * 模拟的任务状态存储
     */
    private final Map<String, ProcessResult> taskStatusMap = new ConcurrentHashMap<>();
    
    private final Random random = new Random();
    
    @Override
    public ProcessResult queryStatus(String taskId) {
        log.info("查询任务状态: taskId={}", taskId);
        
        ProcessResult result = taskStatusMap.get(taskId);
        if (result != null) {
            return result;
        }
        
        // 任务不存在
        return ProcessResult.builder()
                .taskId(taskId)
                .success(false)
                .message("任务不存在: " + taskId)
                .processTime(LocalDateTime.now())
                .build();
    }
    
    @Override
    public ProcessResult processData(ProcessRequest request) {
        long startTime = System.currentTimeMillis();
        String taskId = request.getTaskId();
        
        log.info("开始处理数据: taskId={}, type={}, dataSource={}", 
                taskId, request.getType(), request.getDataSource());
        
        try {
            // 模拟耗时处理
            int delaySeconds = request.getDelaySeconds();
            if (delaySeconds > 0) {
                log.info("模拟耗时处理: {} 秒", delaySeconds);
                simulateProcessing(delaySeconds);
            }
            
            // 执行实际处理逻辑
            Map<String, Object> resultData = executeProcessing(request);
            
            long duration = System.currentTimeMillis() - startTime;
            
            ProcessResult result = ProcessResult.builder()
                    .taskId(taskId)
                    .success(true)
                    .message(String.format("数据处理完成: %s", request.getType().getDescription()))
                    .data(resultData)
                    .processTime(LocalDateTime.now())
                    .durationMs(duration)
                    .processedCount(resultData.containsKey("processedCount") 
                            ? (Integer) resultData.get("processedCount") : 1)
                    .build();
            
            // 保存任务状态
            taskStatusMap.put(taskId, result);
            
            log.info("数据处理完成: taskId={}, duration={}ms", taskId, duration);
            
            return result;
            
        } catch (Exception e) {
            log.error("数据处理失败: taskId={}", taskId, e);
            
            ProcessResult errorResult = ProcessResult.builder()
                    .taskId(taskId)
                    .success(false)
                    .message("处理失败: " + e.getMessage())
                    .processTime(LocalDateTime.now())
                    .durationMs(System.currentTimeMillis() - startTime)
                    .build();
            
            taskStatusMap.put(taskId, errorResult);
            
            return errorResult;
        }
    }
    
    @Override
    public List<ProcessResult> batchProcess(List<ProcessRequest> requests) {
        log.info("开始批量处理: 共 {} 个任务", requests.size());
        
        List<ProcessResult> results = new ArrayList<>();
        
        for (ProcessRequest request : requests) {
            ProcessResult result = processData(request);
            results.add(result);
        }
        
        log.info("批量处理完成: 成功 {}/{}", 
                results.stream().filter(ProcessResult::isSuccess).count(),
                results.size());
        
        return results;
    }
    
    /**
     * 模拟耗时处理
     */
    private void simulateProcessing(int seconds) {
        try {
            // 分段睡眠，模拟处理进度
            for (int i = 0; i < seconds; i++) {
                TimeUnit.SECONDS.sleep(1);
                if ((i + 1) % 5 == 0) {
                    log.debug("处理进度: {}/{} 秒", i + 1, seconds);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("处理被中断", e);
        }
    }
    
    /**
     * 执行实际处理逻辑
     */
    private Map<String, Object> executeProcessing(ProcessRequest request) {
        Map<String, Object> resultData = new HashMap<>();
        ProcessType type = request.getType();
        
        switch (type) {
            case DATA_IMPORT:
                resultData.put("importedRecords", 1000 + random.nextInt(9000));
                resultData.put("skippedRecords", random.nextInt(100));
                resultData.put("processedCount", (Integer) resultData.get("importedRecords"));
                break;
                
            case DATA_EXPORT:
                resultData.put("exportedRecords", 5000 + random.nextInt(5000));
                resultData.put("fileSize", (500 + random.nextInt(500)) + "KB");
                resultData.put("filePath", "/exports/" + request.getTaskId() + ".csv");
                resultData.put("processedCount", (Integer) resultData.get("exportedRecords"));
                break;
                
            case DATA_TRANSFORM:
                resultData.put("transformedRecords", 2000 + random.nextInt(3000));
                resultData.put("transformType", "JSON_TO_XML");
                resultData.put("processedCount", (Integer) resultData.get("transformedRecords"));
                break;
                
            case BATCH_CALCULATION:
                resultData.put("calculatedItems", 10000 + random.nextInt(10000));
                resultData.put("totalSum", random.nextDouble() * 1000000);
                resultData.put("averageValue", random.nextDouble() * 100);
                resultData.put("processedCount", (Integer) resultData.get("calculatedItems"));
                break;
                
            case DATA_CLEANING:
                resultData.put("cleanedRecords", 3000 + random.nextInt(2000));
                resultData.put("removedDuplicates", random.nextInt(500));
                resultData.put("fixedFormats", random.nextInt(200));
                resultData.put("processedCount", (Integer) resultData.get("cleanedRecords"));
                break;
                
            case REPORT_GENERATION:
                resultData.put("reportName", "Report_" + request.getTaskId());
                resultData.put("pages", 10 + random.nextInt(90));
                resultData.put("charts", random.nextInt(20));
                resultData.put("reportPath", "/reports/" + request.getTaskId() + ".pdf");
                resultData.put("processedCount", (Integer) resultData.get("pages"));
                break;
                
            default:
                resultData.put("status", "completed");
                resultData.put("processedCount", 1);
        }
        
        // 添加通用信息
        resultData.put("dataSource", request.getDataSource());
        resultData.put("processType", type.name());
        resultData.put("serverTime", LocalDateTime.now().toString());
        
        return resultData;
    }
}
