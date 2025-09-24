package io.nebula.data.access.exception;

/**
 * 查询异常
 * 当查询构建或执行出现错误时抛出
 * 
 * @author Nebula Framework
 * @since 2.0.0
 */
public class QueryException extends DataAccessException {
    
    private final String queryString;
    private final String queryType;
    
    public QueryException(String message) {
        super(ErrorCodes.INVALID_QUERY, message);
        this.queryString = null;
        this.queryType = null;
    }
    
    public QueryException(String queryType, String queryString, String message) {
        super(ErrorCodes.INVALID_QUERY, message);
        this.queryType = queryType;
        this.queryString = queryString;
    }
    
    public QueryException(String message, Throwable cause) {
        super(ErrorCodes.INVALID_QUERY, message, cause);
        this.queryString = null;
        this.queryType = null;
    }
    
    public QueryException(String queryType, String queryString, String message, Throwable cause) {
        super(ErrorCodes.INVALID_QUERY, message, cause);
        this.queryType = queryType;
        this.queryString = queryString;
    }
    
    /**
     * 获取查询字符串
     */
    public String getQueryString() {
        return queryString;
    }
    
    /**
     * 获取查询类型
     */
    public String getQueryType() {
        return queryType;
    }
    
    /**
     * 便捷的创建方法
     */
    public static QueryException of(String message) {
        return new QueryException(message);
    }
    
    public static QueryException of(String queryType, String queryString, String message) {
        return new QueryException(queryType, queryString, message);
    }
    
    public static QueryException of(String message, Throwable cause) {
        return new QueryException(message, cause);
    }
}
