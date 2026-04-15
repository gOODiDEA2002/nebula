package io.nebula.example.modules.search.entity.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量索引产品 DTO
 *
 * @author nebula
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkIndexProductDto {

    /**
     * 产品ID列表
     */
    @NotEmpty(message = "产品ID列表不能为空")
    private List<Long> productIds;
}

