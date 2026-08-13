package com.shiyq.entity.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增漫画标签请求。
 */
@Data
public class MangaTagCreateDTO {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称不能超过50个字符")
    private String tagName;

    @NotNull(message = "标签分类不能为空")
    @Min(value = 1, message = "标签分类必须在1到6之间")
    @Max(value = 6, message = "标签分类必须在1到6之间")
    private Integer category;
}
