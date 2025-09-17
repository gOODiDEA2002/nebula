package io.nebula.core.common.exception;

/**
 * 系统异常
 * 用于表示系统级错误，如网络连接失败、数据库连接失败等
 */
public class SystemException extends NebulaException {
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param args      错误参数
     */
    public SystemException(String errorCode, String message, Object... args) {
        super(errorCode, message, args);
    }
    
    /**
     * 构造函数（带原因）
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原因
     * @param args      错误参数
     */
    public SystemException(String errorCode, String message, Throwable cause, Object... args) {
        super(errorCode, message, cause, args);
    }
    
    /**
     * 创建系统异常的便捷方法
     * 
     * @param message 错误消息
     * @return 系统异常实例
     */
    public static SystemException of(String message) {
        return new SystemException("SYSTEM_ERROR", message);
    }
    
    /**
     * 创建系统异常的便捷方法（带原因）
     * 
     * @param message 错误消息
     * @param cause   原因
     * @return 系统异常实例
     */
    public static SystemException of(String message, Throwable cause) {
        return new SystemException("SYSTEM_ERROR", message, cause);
    }
    
    /**
     * 创建带错误代码的系统异常
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @return 系统异常实例
     */
    public static SystemException withCode(String errorCode, String message) {
        return new SystemException(errorCode, message);
    }
    
    /**
     * 创建带错误代码的系统异常（带原因）
     * 
     * @param errorCode 错误代码
     * @param message   错误消息
     * @param cause     原因
     * @return 系统异常实例
     */
    public static SystemException withCode(String errorCode, String message, Throwable cause) {
        return new SystemException(errorCode, message, cause);
    }
    
    /**
     * 数据库相关异常
     */
    public static class Database extends SystemException {
        public Database(String message, Throwable cause) {
            super("DATABASE_ERROR", message, cause);
        }
        
        public static Database connectionFailed(Throwable cause) {
            return new Database("数据库连接失败", cause);
        }
        
        public static Database operationFailed(String operation, Throwable cause) {
            return new Database("数据库操作失败: " + operation, cause);
        }
    }
    
    /**
     * 网络相关异常
     */
    public static class Network extends SystemException {
        public Network(String message, Throwable cause) {
            super("NETWORK_ERROR", message, cause);
        }
        
        public static Network connectionTimeout(String host) {
            return new Network("网络连接超时: " + host, null);
        }
        
        public static Network requestFailed(String url, Throwable cause) {
            return new Network("网络请求失败: " + url, cause);
        }
    }
    
    /**
     * 配置相关异常
     */
    public static class Configuration extends SystemException {
        public Configuration(String message) {
            super("CONFIG_ERROR", message);
        }
        
        public static Configuration missing(String key) {
            return new Configuration("缺少必需的配置项: " + key);
        }
        
        public static Configuration invalid(String key, String value) {
            return new Configuration("配置项值无效: " + key + " = " + value);
        }
    }
}
