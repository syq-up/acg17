package com.shiyq.entity.DTO;

import lombok.Data;

/**
 * 游戏基础信息更新请求。文件、归属、删除和收藏状态由各自接口维护。
 */
@Data
public class GameUpdateDTO {
    private String title;
    private String chineseTitle;
    private String version;
    private String description;
}
