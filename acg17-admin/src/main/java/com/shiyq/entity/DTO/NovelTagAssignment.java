package com.shiyq.entity.DTO;

import lombok.Data;

/**
 * 查询小说标签时使用的轻量结果对象。
 */
@Data
public class NovelTagAssignment {
    private Integer novelId;
    private String name;
}
