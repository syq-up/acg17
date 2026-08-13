package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新小说章节请求。
 */
@Data
public class NovelChapterUpdateDTO {
    @NotNull(message = "章节ID不能为空")
    @Positive(message = "章节ID必须大于0")
    private Integer id;

    @NotBlank(message = "章节名不能为空")
    @Size(max = 150, message = "章节名不能超过150个字符")
    private String title;

    @Size(max = 10000, message = "章节段落不能超过10000段")
    private List<@NotNull(message = "章节段落不能为空")
            @Size(max = 100000, message = "单个章节段落不能超过100000个字符") String> content;
}
