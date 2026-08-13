package com.shiyq.mapper;

import com.shiyq.entity.DO.Illustration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Mapper
public interface IllustrationMapper extends BaseMapper<Illustration> {

    /**
     * 根据条件，分页查询插画作品
     * @param pageNum   条件之一，页数
     * @param deleted   条件之一，查询 未 逻辑删除的记录（0否1是）
     */
    List<Illustration> getListByCondition(@Param("userId") Integer userId,
                                          @Param("pageNum") Long pageNum,
                                          @Param("pageSize") Long pageSize,
                                          @Param("deleted") Boolean deleted);

    /**
     * 根据条件，查询插画作品总数
     * @param deleted 是否查询已逻辑删除的记录总数（0否1是）
     * @return 记录总数
     */
    long getTotalByCondition(@Param("userId") Integer userId, @Param("deleted") Boolean deleted);

    /**
     * 随机获取一张插画作品
     */
    Illustration getRandomRecord();

    /**
     * 逻辑删除当前用户的插画
     */
    int deleteByIdAndUserId(@Param("id") int id, @Param("userId") int userId);

    /**
     * 查询达到回收站保留期限的插画ID
     */
    List<Integer> getExpiredIds(@Param("cutoff") Date cutoff);

    /**
     * 锁定并读取仍满足清理条件的插画
     */
    Illustration selectExpiredByIdForUpdate(@Param("id") int id, @Param("cutoff") Date cutoff);

    /**
     * 物理删除仍满足清理条件的插画
     */
    int realDeleteExpiredById(@Param("id") int id, @Param("cutoff") Date cutoff);

    /**
     * 恢复逻辑删除的记录
     */
    int restoreByIdAndUserId(@Param("id") int id, @Param("userId") int userId);

    /**
     * 锁定并读取当前用户的一条有效插画
     */
    Illustration selectActiveByIdForUpdate(@Param("id") int id, @Param("userId") int userId);

    /**
     * 将排序区间整体减一，包含逻辑删除记录
     */
    int decrementSortOrderRange(@Param("userId") int userId,
                                @Param("oldSortOrder") int oldSortOrder,
                                @Param("targetSortOrder") int targetSortOrder);

    /**
     * 将排序区间整体加一，包含逻辑删除记录
     */
    int incrementSortOrderRange(@Param("userId") int userId,
                                @Param("targetSortOrder") int targetSortOrder,
                                @Param("oldSortOrder") int oldSortOrder);

    /**
     * 将待移动记录暂存到保留序号 0，为区间移动腾出目标序号。
     */
    int moveSortOrderToTemporary(@Param("id") int id, @Param("userId") int userId);

    /**
     * 更新当前用户有效插画的排序序号
     */
    int updateSortOrderByIdAndUserId(@Param("id") int id,
                                     @Param("sortOrder") int sortOrder,
                                     @Param("userId") int userId);

    /**
     * 获取当前用户所有插画的最大排序序号
     */
    int getMaxSortOrder(@Param("userId") int userId);

    /**
     * 获取所有插画作品
     */
    List<Illustration> getAll();
}
