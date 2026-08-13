package com.shiyq.entity.DTO;

import lombok.Data;

/**
 * 漫画基础信息更新请求。文件、标签、归属和状态由各自接口维护。
 */
@Data
public class MangaUpdateDTO {
    private String title;
    private String chineseTitle;
    private String description;
    private String author;
}
