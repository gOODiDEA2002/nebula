package io.nebula.task.core;

/**
 * 任务执行日志接口
 */
public interface TaskLogger {
    
    /**
     * 记录信息日志
     * 
     * @param message 日志消息
     * @param args 参数
     */
    void info(String message, Object... args);
    
    /**
     * 记录警告日志
     * 
     * @param message 日志消息
     * @param args 参数
     */
    void warn(String message, Object... args);
    
    /**
     * 记录错误日志
     * 
     * @param message 日志消息
     * @param args 参数
     */
    void error(String message, Object... args);
    
    /**
     * 记录错误日志
     * 
     * @param message 日志消息
     * @param throwable 异常
     */
    void error(String message, Throwable throwable);
    
    /**
     * 获取日志内容
     * 
     * @return 日志内容
     */
    String getLogContent();
    
    /**
     * 获取日志行数
     * 
     * @return 日志行数
     */
    int getLogLines();
}
