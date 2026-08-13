package com.shiyq.entity.DTO;

import lombok.Data;

import java.util.List;

/**
 * 更新小说章节请求。
 */
@Data
public class NovelChapterUpdateDTO {
    private Integer id;
    private String title;
    private List<String> content;
}
