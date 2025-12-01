package io.nebula.rpc.core.context;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC 上下文
 * <p>
 * 用于在 RPC 调用链中传递上下文信息。
 * 提供通用的 metadata 传递机制，不关心业务含义。
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 调用方设置
 * RpcContext.set("X-Trace-Id", traceId);
 * 
 * // 服务方读取
 * String traceId = RpcContext.get("X-Trace-Id");
 * }</pre>
 *
 * @author Nebula Framework
 * @since 2.0.1
 */
public class RpcContext {

    private static final ThreadLocal<Map<String, String>> METADATA = ThreadLocal.withInitial(HashMap::new);

    /**
     * 设置元数据
     *
     * @param key   键
     * @param value 值
     */
    public static void set(String key, String value) {
        if (key != null && value != null) {
            METADATA.get().put(key, value);
        }
    }

    /**
     * 获取元数据
     *
     * @param key 键
     * @return 值，不存在返回 null
     */
    public static String get(String key) {
        return METADATA.get().get(key);
    }

    /**
     * 获取所有元数据
     *
     * @return 元数据副本
     */
    public static Map<String, String> getAll() {
        return new HashMap<>(METADATA.get());
    }

    /**
     * 设置所有元数据（批量）
     *
     * @param metadata 元数据
     */
    public static void setAll(Map<String, String> metadata) {
        if (metadata != null) {
            METADATA.get().putAll(metadata);
        }
    }

    /**
     * 检查是否包含指定键
     *
     * @param key 键
     * @return 是否存在
     */
    public static boolean contains(String key) {
        return METADATA.get().containsKey(key);
    }

    /**
     * 移除指定键
     *
     * @param key 键
     */
    public static void remove(String key) {
        METADATA.get().remove(key);
    }

    /**
     * 清除所有上下文
     */
    public static void clear() {
        METADATA.remove();
    }
}




