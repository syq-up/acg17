package com.shiyq.entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 漫画页在 pages JSON 中的持久化结构。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MangaPageData {

    private int page;
    private String path;
}
