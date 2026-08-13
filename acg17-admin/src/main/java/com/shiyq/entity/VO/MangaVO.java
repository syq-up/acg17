package com.shiyq.entity.VO;

import lombok.Data;

@Data
public class MangaVO {
    private Integer id;
    private String title;
    private String chineseTitle;
    private String cover;
    private boolean favorite;
}
