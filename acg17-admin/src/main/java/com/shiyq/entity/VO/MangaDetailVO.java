package com.shiyq.entity.VO;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class MangaDetailVO {
    private Long id;
    private String title;
    private String chineseTitle;
    private String cover;
    private String description;
    private String author;
    private List<Map<String, Object>> pages;
    private List<MangaTagVO> characterTags;
    private List<MangaTagVO> maleTags;
    private List<MangaTagVO> femaleTags;
    private List<MangaTagVO> mixedTags;
    private List<MangaTagVO> otherTags;
    private List<MangaTagVO> originalTags;
    private boolean favorite;
    private boolean deleted;
    private String updateTime;
}
