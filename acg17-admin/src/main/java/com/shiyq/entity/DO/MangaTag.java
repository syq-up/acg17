package com.shiyq.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 漫画标签表
 * </p>
 *
 * @author shiyq
 * @since 2024-01-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("manga_tag")
public class MangaTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签ID，自增主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 标签所属用户ID
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 标签名称
     */
    @TableField("tag_name")
    private String tagName;

    /**
     * 分类标记，1-角色，2-男性，3-女性，4-混合，5-其他，6-原作
     */
    @TableField("category")
    private Integer category;

}
