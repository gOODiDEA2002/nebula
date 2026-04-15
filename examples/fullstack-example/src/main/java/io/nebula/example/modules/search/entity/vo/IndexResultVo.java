package io.nebula.example.modules.search.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 索引操作结果 VO
 *
 * @author nebula
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndexResultVo {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 索引名称
     */
    private String indexName;

    /**
     * 文档ID
     */
    private String documentId;

    /**
     * 错误消息
     */
    private String errorMessage;
}

