package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 新增漫画章节请求。
 */
@Data
public class MangaChapterUploadDTO {
    @NotBlank(message = "章节标题不能为空")
    @Size(max = 150, message = "章节标题不能超过150个字符")
    private String title;

    @NotNull(message = "章节压缩包不能为空")
    private MultipartFile file;
}
