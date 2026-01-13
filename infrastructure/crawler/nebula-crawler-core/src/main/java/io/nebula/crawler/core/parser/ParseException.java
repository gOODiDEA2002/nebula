package io.nebula.crawler.core.parser;

import io.nebula.crawler.core.exception.CrawlerException;

/**
 * 解析异常
 *
 * @author Nebula Team
 * @since 2.0.1
 */
public class ParseException extends CrawlerException {
    
    public ParseException(String message) {
        super("PARSE_ERROR", message);
    }
    
    public ParseException(String message, Throwable cause) {
        super("PARSE_ERROR", message, cause);
    }
    
    /**
     * 创建内容为空异常
     */
    public static ParseException emptyContent() {
        return new ParseException("响应内容为空");
    }
    
    /**
     * 创建格式错误异常
     */
    public static ParseException invalidFormat(String expected, String actual) {
        return new ParseException(
            String.format("内容格式错误: 期望 %s, 实际 %s", expected, actual)
        );
    }
    
    /**
     * 创建字段缺失异常
     */
    public static ParseException missingField(String fieldName) {
        return new ParseException("缺少必需字段: " + fieldName);
    }
}

