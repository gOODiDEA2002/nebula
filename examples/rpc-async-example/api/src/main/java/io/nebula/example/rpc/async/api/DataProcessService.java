package io.nebula.example.rpc.async.api;

import io.nebula.example.rpc.async.api.dto.ProcessRequest;
import io.nebula.example.rpc.async.api.dto.ProcessResult;

import java.util.List;

/**
 * 数据处理服务接口（同步版本）
 * 
 * <p>服务端实现此接口，提供实际的业务处理逻辑。
 * 所有方法都是同步的，异步包装由客户端框架处理。
 * 
 * @author Nebula Framework
 */
public interface DataProcessService {
    
    /**
     * 查询任务状态
     * 
     * @param taskId 任务ID
     * @return 处理结果
     */
    ProcessResult queryStatus(String taskId);
    
    /**
     * 处理单条数据（同步方法）
     * 
     * <p>对应客户端的异步方法 processDataAsync
     * 
     * @param request 处理请求
     * @return 处理结果
     */
    ProcessResult processData(ProcessRequest request);
    
    /**
     * 批量处理数据（同步方法）
     * 
     * <p>对应客户端的异步方法 batchProcessAsync
     * 
     * @param requests 请求列表
     * @return 结果列表
     */
    List<ProcessResult> batchProcess(List<ProcessRequest> requests);
}
