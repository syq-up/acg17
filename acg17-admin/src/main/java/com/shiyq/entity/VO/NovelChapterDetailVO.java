package com.shiyq.entity.VO;

import lombok.Data;

import java.util.List;

@Data
public class NovelChapterDetailVO {
    private Integer id;
    private Integer novelId;
    private String title;
    private List<String> content;
    private String totalWords;
    private Integer sortOrder;
    private String updateTime;
}
