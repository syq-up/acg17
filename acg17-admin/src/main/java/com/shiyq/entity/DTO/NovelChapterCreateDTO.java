package com.shiyq.entity.DTO;

import lombok.Data;

import java.util.List;

/**
 * 新增小说章节请求。
 */
@Data
public class NovelChapterCreateDTO {
    private Integer novelId;
    private String title;
    private List<String> content;
}
