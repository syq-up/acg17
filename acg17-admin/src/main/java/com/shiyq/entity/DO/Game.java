package com.shiyq.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 游戏信息实体类
 *
 * @author shiyq
 * @since 2024-12-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "game", autoResultMap = true)
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 游戏ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 游戏名称
     */
    @TableField("title")
    private String title;

    /**
     * 中文名称
     */
    @TableField("chinese_title")
    private String chineseTitle;

    /**
     * 版本号
     */
    @TableField("version")
    private String version;

    /**
     * 游戏封面路径
     */
    @TableField("cover")
    private String cover;

    /**
     * 游戏图标路径
     */
    @TableField("icon")
    private String icon;

    /**
     * 游戏简介
     */
    @TableField("description")
    private String description;

    /**
     * 游戏预览图，存储多张预览图路径的JSON数组
     */
    @TableField(value = "preview_images", typeHandler = com.shiyq.handler.StringListTypeHandler.class)
    private List<String> previewImages;

    /**
     * 收藏状态：0-未收藏，1-已收藏
     */
    @TableField("favorite")
    private Boolean favorite;

    /**
     * 逻辑删除标记：0-未删除，1-已删除
     */
    @TableField("deleted")
    @TableLogic
    private Boolean deleted;

    /**
     * 用户ID，关联用户表
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
