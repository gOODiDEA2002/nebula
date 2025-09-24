package io.nebula.rpc.core.message;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * RPC响应对象
 */
@Data
@Builder
public class RpcResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 响应状态码
     */
    private int statusCode;
    
    /**
     * 响应消息
     */
    private String message;
    
    /**
     * 响应结果
     */
    private Object result;
    
    /**
     * 异常信息
     */
    private Throwable exception;
    
    /**
     * 响应头
     */
    private Map<String, String> headers;
    
    /**
     * 响应时间戳
     */
    private long timestamp;
    
    /**
     * 处理耗时（毫秒）
     */
    private long processingTime;
    
    /**
     * 创建成功响应
     * 
     * @param requestId 请求ID
     * @param result    结果
     * @return 响应对象
     */
    public static RpcResponse success(String requestId, Object result) {
        return RpcResponse.builder()
                .requestId(requestId)
                .statusCode(200)
                .message("Success")
                .result(result)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建错误响应
     * 
     * @param requestId 请求ID
     * @param message   错误消息
     * @return 响应对象
     */
    public static RpcResponse error(String requestId, String message) {
        return RpcResponse.builder()
                .requestId(requestId)
                .statusCode(500)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 创建异常响应
     * 
     * @param requestId 请求ID
     * @param exception 异常
     * @return 响应对象
     */
    public static RpcResponse exception(String requestId, Throwable exception) {
        return RpcResponse.builder()
                .requestId(requestId)
                .statusCode(500)
                .message(exception.getMessage())
                .exception(exception)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    /**
     * 是否成功
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }
}
