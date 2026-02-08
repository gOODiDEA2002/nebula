package io.nebula.example.rpc.async.api;

import io.nebula.example.rpc.async.api.dto.ProcessRequest;
import io.nebula.example.rpc.async.api.dto.ProcessResult;
import io.nebula.rpc.async.annotation.AsyncRpc;
import io.nebula.rpc.async.execution.AsyncRpcResult;
import io.nebula.rpc.core.annotation.RpcClient;

import java.util.List;

/**
 * 数据处理RPC客户端接口
 * 
 * <p>继承 {@link DataProcessService}，同时提供异步调用版本。
 * 
 * <p>使用示例:
 * <pre>{@code
 * @Autowired
 * private DataProcessRpcClient rpcClient;
 * 
 * // 同步调用
 * ProcessResult result = rpcClient.processData(request);
 * 
 * // 异步调用
 * AsyncRpcResult<ProcessResult> asyncResult = rpcClient.processDataAsync(request);
 * String executionId = asyncResult.getExecutionId();
 * 
 * // 查询执行状态
 * AsyncRpcExecution execution = executionManager.getExecution(executionId);
 * }</pre>
 * 
 * @author Nebula Framework
 */
@RpcClient("data-process-service")
public interface DataProcessRpcClient extends DataProcessService {
    
    /**
     * 异步处理单条数据
     * 
     * <p>框架会自动将此方法映射到服务端的 {@link #processData(ProcessRequest)} 方法。
     * 调用后立即返回，实际处理在后台异步执行。
     * 
     * @param request 处理请求
     * @return 异步结果（包含executionId用于查询状态）
     */
    @AsyncRpc(timeout = 600)  // 10分钟超时
    AsyncRpcResult<ProcessResult> processDataAsync(ProcessRequest request);
    
    /**
     * 异步批量处理数据
     * 
     * <p>框架会自动将此方法映射到服务端的 {@link #batchProcess(List)} 方法。
     * 适用于大批量数据处理场景。
     * 
     * @param requests 请求列表
     * @return 异步结果
     */
    @AsyncRpc(timeout = 1200)  // 20分钟超时
    AsyncRpcResult<List<ProcessResult>> batchProcessAsync(List<ProcessRequest> requests);
}
