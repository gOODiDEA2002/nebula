package io.nebula.example.modules.search.entity.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 产品搜索建议 DTO
 *
 * @author nebula
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuggestProductDto {

    /**
     * 搜索文本
     */
    @NotBlank(message = "搜索文本不能为空")
    private String text;

    /**
     * 返回数量
     */
    @Min(value = 1, message = "返回数量不能小于1")
    @Max(value = 10, message = "返回数量不能大于10")
    private Integer size = 5;
}

