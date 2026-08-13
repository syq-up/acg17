package com.shiyq.entity.DO;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
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
@TableName(value = "novel_chapter", autoResultMap = true)
public class NovelChapter implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id（章节id）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 章节标题
     */
    @TableField("title")
    private String title;

    /**
     * 书籍id（对应哪本书）
     */
    @TableField("novel_id")
    private Integer novelId;

    /**
     * 章节正文，按段落保存为 JSON 数组
     */
    @TableField(value = "content", typeHandler = com.shiyq.handler.StringListTypeHandler.class)
    private List<String> content;

    /**
     * 章节字数
     */
    @TableField("total_words")
    private Integer totalWords;

    /**
     * 同一本小说内的章节顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
