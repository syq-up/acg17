package com.shiyq.entity.DTO;

import lombok.Data;

import java.util.List;

/**
 * 新增小说请求。
 */
@Data
public class NovelCreateDTO {
    private String title;
    private String author;
    private List<String> tags;
}
