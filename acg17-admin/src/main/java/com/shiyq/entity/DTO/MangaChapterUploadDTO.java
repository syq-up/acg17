package com.shiyq.entity.DTO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 新增漫画章节请求。
 */
@Data
public class MangaChapterUploadDTO {
    private String title;
    private MultipartFile file;
}
