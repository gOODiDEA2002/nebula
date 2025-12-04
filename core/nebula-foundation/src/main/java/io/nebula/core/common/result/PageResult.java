package io.nebula.core.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分页结果
 * 
 * @param <T> 数据类型
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResult<T> extends Result<List<T>> {
    
    /**
     * 分页信息
     */
    private PageInfo pageInfo;
    
    /**
     * 构造函数
     */
    public PageResult() {
        super(false, null, null, null, null, null);
    }
    
    /**
     * 构造函数
     * 
     * @param success   成功标识
     * @param code      响应代码
     * @param message   响应消息
     * @param data      数据列表
     * @param pageInfo  分页信息
     * @param timestamp 时间戳
     * @param requestId 请求ID
     */
    @Builder(builderMethodName = "pageBuilder")
    public PageResult(boolean success, String code, String message, List<T> data, 
                     PageInfo pageInfo, LocalDateTime timestamp, String requestId) {
        super(success, code, message, data, timestamp, requestId);
        this.pageInfo = pageInfo;
    }
    
    /**
     * 创建成功的分页响应
     * 
     * @param data     数据列表
     * @param pageInfo 分页信息
     * @param <T>      数据类型
     * @return 分页响应
     */
    public static <T> PageResult<T> success(List<T> data, PageInfo pageInfo) {
        return PageResult.<T>pageBuilder()
                .success(true)
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .pageInfo(pageInfo)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建成功的分页响应（带消息）
     * 
     * @param data     数据列表
     * @param pageInfo 分页信息
     * @param message  响应消息
     * @param <T>      数据类型
     * @return 分页响应
     */
    public static <T> PageResult<T> success(List<T> data, PageInfo pageInfo, String message) {
        return PageResult.<T>pageBuilder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .pageInfo(pageInfo)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 创建成功的分页响应（便捷方法）
     * 
     * @param data         数据列表
     * @param pageNumber   页码（从1开始）
     * @param pageSize     页大小
     * @param totalElements 总元素数
     * @param <T>          数据类型
     * @return 分页响应
     */
    public static <T> PageResult<T> success(List<T> data, int pageNumber, int pageSize, long totalElements) {
        PageInfo pageInfo = PageInfo.builder()
                .pageNumber(pageNumber)
                .pageSize(pageSize)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / pageSize))
                .numberOfElements(data != null ? data.size() : 0)
                .hasNext(pageNumber * pageSize < totalElements)
                .hasPrevious(pageNumber > 1)
                .isFirst(pageNumber == 1)
                .isLast(pageNumber * pageSize >= totalElements)
                .build();
        
        return success(data, pageInfo);
    }
    
    /**
     * 创建空的分页响应
     * 
     * @param pageNumber 页码
     * @param pageSize   页大小
     * @param <T>        数据类型
     * @return 空分页响应
     */
    public static <T> PageResult<T> empty(int pageNumber, int pageSize) {
        return success(List.of(), pageNumber, pageSize, 0L);
    }
    
    /**
     * 创建分页错误响应
     * 
     * @param code    错误代码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 分页错误响应
     */
    public static <T> PageResult<T> pageError(String code, String message) {
        return PageResult.<T>pageBuilder()
                .success(false)
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 分页信息
     */
    @Data
    @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageInfo {
        /**
         * 当前页码（从1开始）
         */
        private int pageNumber;
        
        /**
         * 页大小
         */
        private int pageSize;
        
        /**
         * 总元素数
         */
        private long totalElements;
        
        /**
         * 总页数
         */
        private int totalPages;
        
        /**
         * 是否有下一页
         */
        private boolean hasNext;
        
        /**
         * 是否有上一页
         */
        private boolean hasPrevious;
        
        /**
         * 是否是第一页
         */
        private boolean isFirst;
        
        /**
         * 是否是最后一页
         */
        private boolean isLast;
        
        /**
         * 当前页元素数量
         */
        private int numberOfElements;
        
        /**
         * 创建分页信息
         * 
         * @param pageNumber    页码
         * @param pageSize      页大小
         * @param totalElements 总元素数
         * @return 分页信息
         */
        public static PageInfo of(int pageNumber, int pageSize, long totalElements) {
            return PageInfo.builder()
                    .pageNumber(pageNumber)
                    .pageSize(pageSize)
                    .totalElements(totalElements)
                    .totalPages((int) Math.ceil((double) totalElements / pageSize))
                    .hasNext(pageNumber * pageSize < totalElements)
                    .hasPrevious(pageNumber > 1)
                    .isFirst(pageNumber == 1)
                    .isLast(pageNumber * pageSize >= totalElements)
                    .build();
        }
        
        /**
         * 创建分页信息（带当前页元素数量）
         * 
         * @param pageNumber       页码
         * @param pageSize         页大小
         * @param totalElements    总元素数
         * @param numberOfElements 当前页元素数量
         * @return 分页信息
         */
        public static PageInfo of(int pageNumber, int pageSize, long totalElements, int numberOfElements) {
            PageInfo pageInfo = of(pageNumber, pageSize, totalElements);
            pageInfo.setNumberOfElements(numberOfElements);
            return pageInfo;
        }
    }
}
