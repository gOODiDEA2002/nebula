package io.nebula.rpc.core.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * RPC请求对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 请求ID
     */
    private String requestId;
    
    /**
     * 服务名称
     */
    private String serviceName;
    
    /**
     * 方法名
     */
    private String methodName;
    
    /**
     * 参数类型的全限定类名（仅用于服务端方法匹配，按名字比对，不做 Class.forName）。
     * 用字符串而非 {@code Class<?>[]}：避免 Jackson 反序列化时对请求携带的任意类名调用 Class.forName
     * 造成任意类加载。线格式与原来一致（Jackson 本就把 Class 序列化为类名字符串）。
     */
    private String[] parameterTypes;
    
    /**
     * 参数值
     */
    private Object[] parameters;
    
    /**
     * 请求头
     */
    private Map<String, String> headers;
    
    /**
     * 请求时间戳
     */
    private long timestamp;
    
    /**
     * 超时时间（毫秒）
     */
    private long timeout;
    
    /**
     * 版本号
     */
    private String version;
}
