package com.shiyq.entity.VO;

import lombok.Data;

@Data
public class MangaTagVO {
    private Integer tagId;
    private String raw;
    private String name;
    private String intro;
    private String links;
    private Integer category;
    private Integer tagCount;
}
