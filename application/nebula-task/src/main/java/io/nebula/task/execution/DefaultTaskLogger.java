package io.nebula.task.execution;

import io.nebula.task.core.TaskLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/**
 * 默认任务日志实现
 * 同时输出到标准日志和内存缓冲区
 */
public class DefaultTaskLogger implements TaskLogger {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultTaskLogger.class);
    
    private final int logId;
    private final long logDateTime;
    private final StringBuilder logBuffer;
    private int logLines;
    
    public DefaultTaskLogger(int logId, long logDateTime) {
        this.logId = logId;
        this.logDateTime = logDateTime;
        this.logBuffer = new StringBuilder(1024);
        this.logLines = 0;
    }
    
    @Override
    public void info(String message, Object... args) {
        FormattingTuple tuple = MessageFormatter.format(message, args);
        String formattedMessage = tuple.getMessage();
        
        // 输出到标准日志
        logger.info(formattedMessage);
        
        // 保存到内存缓冲区
        appendToBuffer("INFO", formattedMessage);
    }
    
    @Override
    public void warn(String message, Object... args) {
        FormattingTuple tuple = MessageFormatter.format(message, args);
        String formattedMessage = tuple.getMessage();
        
        // 输出到标准日志
        logger.warn(formattedMessage);
        
        // 保存到内存缓冲区
        appendToBuffer("WARN", formattedMessage);
    }
    
    @Override
    public void error(String message, Object... args) {
        FormattingTuple tuple = MessageFormatter.format(message, args);
        String formattedMessage = tuple.getMessage();
        
        // 输出到标准日志
        logger.error(formattedMessage);
        
        // 保存到内存缓冲区
        appendToBuffer("ERROR", formattedMessage);
    }
    
    @Override
    public void error(String message, Throwable throwable) {
        // 输出到标准日志
        logger.error(message, throwable);
        
        // 保存到内存缓冲区
        appendToBuffer("ERROR", message + " - " + throwable.getMessage());
        
        // 添加堆栈跟踪
        if (throwable != null) {
            for (StackTraceElement element : throwable.getStackTrace()) {
                appendToBuffer("ERROR", "    at " + element.toString());
            }
        }
    }
    
    @Override
    public String getLogContent() {
        return logBuffer.toString();
    }
    
    @Override
    public int getLogLines() {
        return logLines;
    }
    
    public int getLogId() {
        return logId;
    }
    
    public long getLogDateTime() {
        return logDateTime;
    }
    
    /**
     * 添加日志到缓冲区
     */
    private void appendToBuffer(String level, String message) {
        synchronized (logBuffer) {
            logBuffer.append(String.format("[%s] %s%n", level, message));
            logLines++;
        }
    }
    
    /**
     * 清空日志缓冲区
     */
    public void clear() {
        synchronized (logBuffer) {
            logBuffer.setLength(0);
            logLines = 0;
        }
    }
}
