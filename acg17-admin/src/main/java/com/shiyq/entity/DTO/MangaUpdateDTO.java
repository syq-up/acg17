package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 漫画基础信息更新请求。文件、标签、归属和状态由各自接口维护。
 */
@Data
public class MangaUpdateDTO {
    @NotBlank(message = "漫画标题不能为空")
    @Size(max = 255, message = "漫画标题不能超过255个字符")
    private String title;

    @Size(max = 255, message = "漫画中文标题不能超过255个字符")
    private String chineseTitle;

    @Size(max = 10000, message = "漫画简介不能超过10000个字符")
    private String description;

}
