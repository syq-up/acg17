package com.shiyq.entity.VO;

import com.shiyq.entity.DTO.MangaChapterData;
import lombok.Data;

import java.util.List;

@Data
public class MangaDetailVO {
    private Long id;
    private String title;
    private String chineseTitle;
    private String cover;
    private String description;
    private List<MangaChapterData> pages;
    private List<MangaTagVO> characterTags;
    private List<MangaTagVO> maleTags;
    private List<MangaTagVO> femaleTags;
    private List<MangaTagVO> mixedTags;
    private List<MangaTagVO> otherTags;
    private List<MangaTagVO> originalTags;
    private List<MangaTagVO> artistTags;
    private List<MangaTagVO> groupTags;
    private boolean favorite;
    private boolean deleted;
    private String updateTime;
}
