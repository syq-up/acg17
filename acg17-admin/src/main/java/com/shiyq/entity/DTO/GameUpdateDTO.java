package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 游戏基础信息更新请求。文件、归属、删除和收藏状态由各自接口维护。
 */
@Data
public class GameUpdateDTO {
    @NotBlank(message = "游戏名称不能为空")
    @Size(max = 255, message = "游戏名称不能超过255个字符")
    private String title;

    @Size(max = 255, message = "游戏中文名称不能超过255个字符")
    private String chineseTitle;

    @Size(max = 50, message = "游戏版本不能超过50个字符")
    private String version;

    @Size(max = 10000, message = "游戏简介不能超过10000个字符")
    private String description;
}
