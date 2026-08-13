package com.shiyq.entity.VO;

import lombok.Data;

/**
 * 新增漫画章节结果。
 */
@Data
public class MangaChapterVO {
    private Integer chapter;
    private String title;
    private Integer pageCount;
}
