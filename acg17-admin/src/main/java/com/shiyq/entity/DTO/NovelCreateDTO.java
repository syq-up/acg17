package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 新增小说请求。
 */
@Data
public class NovelCreateDTO {
    @NotBlank(message = "书名不能为空")
    @Size(max = 100, message = "书名不能超过100个字符")
    private String title;

    @Size(max = 100, message = "作者名不能超过100个字符")
    private String author;

    @Size(max = 30, message = "小说标签不能超过30个")
    private List<@NotBlank(message = "标签名不能为空")
            @Size(max = 32, message = "标签名不能超过32个字符") String> tags;
}
