package com.shiyq.entity.VO;

import lombok.Data;

import java.util.List;

@Data
public class NovelVO {
    private Integer id;
    private String title;
    private String author;
    private List<String> tags;
    private String totalWords;
    private String updateTime;
}
