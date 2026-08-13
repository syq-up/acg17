package com.shiyq.mapper;

import com.shiyq.entity.DO.Novel;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
public interface NovelMapper extends BaseMapper<Novel> {
    /**
     * 根据条件，分页查询插画作品
     * @param pageNum   条件之一，页数
     * @param deleted   条件之一，查询 未 逻辑删除的记录（0否1是）
     */
    List<Novel> getListByCondition(@Param("userId") Integer userId,
                                   @Param("pageNum") Long pageNum,
                                   @Param("pageSize") Long pageSize,
                                   @Param("deleted") Boolean deleted,
                                   @Param("tagId") Integer tagId,
                                   @Param("keyword") String keyword);

    /**
     * 根据条件，查询插画作品总数
     * @param deleted 是否查询已逻辑删除的记录总数（0否1是）
     * @return 记录总数
     */
    long getTotalByCondition(@Param("userId") Integer userId,
                             @Param("deleted") Boolean deleted,
                             @Param("tagId") Integer tagId,
                             @Param("keyword") String keyword);

    /**
     * 查询当前用户未删除的小说
     */
    Novel selectOwnedById(@Param("id") int id, @Param("userId") int userId);

    /**
     * 锁定当前用户未删除的小说，串行化同一本小说的章节序号分配。
     */
    Novel selectOwnedByIdForUpdate(@Param("id") int id, @Param("userId") int userId);

    /**
     * 逻辑删除当前用户的小说
     */
    int deleteByIdAndUserId(@Param("id") int id, @Param("userId") int userId);

    /**
     * 恢复当前用户已逻辑删除的小说
     */
    int restoreByIdAndUserId(@Param("id") int id, @Param("userId") int userId);

    /**
     * 更新小说总字数
     */
    int updateTotalWordsByIncrease(@Param("id") int id,
                                   @Param("userId") int userId,
                                   @Param("increase") int increase);
}
