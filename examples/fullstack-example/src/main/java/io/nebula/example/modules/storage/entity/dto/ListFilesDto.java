package io.nebula.example.modules.storage.entity.dto;

import io.nebula.example.modules.storage.entity.vo.FileInfoVo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 列出文件DTO
 */
public class ListFilesDto {
    
    /**
     * 列出文件请求
     */
    @Data
    public static class Request {
        /**
         * 存储桶名称
         */
        @NotBlank(message = "存储桶名称不能为空")
        private String bucket;
        
        /**
         * 前缀过滤
         */
        private String prefix;
        
        /**
         * 最大返回数量
         */
        @Min(value = 1, message = "最大返回数量不能小于1")
        @Max(value = 1000, message = "最大返回数量不能大于1000")
        private Integer maxKeys = 100;
        
        /**
         * 分页标记（上一页最后一个对象的key）
         */
        private String marker;
    }
    
    /**
     * 列出文件响应
     */
    @Data
    public static class Response {
        /**
         * 文件列表
         */
        private List<FileInfoVo> files;
        
        /**
         * 总数
         */
        private Integer total;
        
        /**
         * 是否还有更多数据
         */
        private Boolean hasMore;
        
        /**
         * 下一页标记
         */
        private String nextMarker;
    }
}

