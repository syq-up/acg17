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
@TableName("illustration")
public class Illustration implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 插画的路径地址
     */
    @TableField("path")
    private String path;

    /**
     * 插画大小
     */
    @TableField("size")
    private Integer size;

    /**
     * 宽高比（宽/高）
     */
    @TableField("ratio")
    private Double ratio;

    /**
     * 排序序号，数值越大越靠前
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 逻辑删除（0否1是）
     */
    @TableField("deleted")
    @TableLogic
    private Boolean deleted;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    public Illustration() {
    }

    public Illustration(Integer id) {
        this.id = id;
    }

    public Illustration(String path, Integer size, Integer userId, Double ratio, Integer sortOrder) {
        this.path = path;
        this.size = size;
        this.userId = userId;
        this.ratio = ratio;
        this.sortOrder = sortOrder;
    }
}
