package com.shiyq.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("manga")
public class Manga implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 用户ID，关联用户表
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 标题
     */
    @TableField("title")
    private String title;

    /**
     * 中文标题
     */
    @TableField("chinese_title")
    private String chineseTitle;

    /**
     * 简介
     */
    @TableField("description")
    private String description;

    /**
     * 页数据，存储所有话(集)和对应的页集合
     */
    // @TableField(value = "pages", typeHandler = JacksonTypeHandler.class)
    @TableField(value = "pages")
    private String pages;

    /**
     * 大小，单位为字节
     */
    @TableField("size")
    private Long size;

    /**
     * 收藏状态，0-未收藏，1-已收藏
     */
    @TableField("favorite")
    private Boolean favorite;

    /**
     * 逻辑删除标记，0-未删除，1-已删除
     */
    @TableField("deleted")
    @TableLogic
    private Boolean deleted;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


}
