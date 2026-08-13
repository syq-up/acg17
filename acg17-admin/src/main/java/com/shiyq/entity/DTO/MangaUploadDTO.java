package com.shiyq.entity.DTO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class MangaUploadDTO {
    private String title;
    private String chineseTitle;
    private String description;
    private String author;
    private String tags;
    private MultipartFile file;
}
