package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MangaUploadDTO {
    @NotBlank(message = "漫画标题不能为空")
    @Size(max = 255, message = "漫画标题不能超过255个字符")
    private String title;

    @Size(max = 255, message = "漫画中文标题不能超过255个字符")
    private String chineseTitle;

    @Size(max = 10000, message = "漫画简介不能超过10000个字符")
    private String description;

    @Size(max = 100, message = "漫画作者不能超过100个字符")
    private String author;

    @Size(max = 16384, message = "漫画标签数据过长")
    private String tags;

    @NotNull(message = "漫画压缩包不能为空")
    private MultipartFile file;
}
