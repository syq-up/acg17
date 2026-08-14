package com.shiyq.entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 漫画章节在 pages JSON 中的持久化结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MangaChapterData {

    private int chapter;
    private String title;
    private List<MangaPageData> pagelist = new ArrayList<>();
}
