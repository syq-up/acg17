package com.shiyq.mapper;

import com.shiyq.entity.DO.NovelChapter;
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
public interface NovelChapterMapper extends BaseMapper<NovelChapter> {

    /**
     * 查询小说章节列表
     */
    List<NovelChapter> getList(@Param("novelId") int novelId, @Param("userId") int userId);

    /**
     * 查询当前用户拥有的章节及其小说ID
     */
    NovelChapter selectOwnedById(@Param("id") int id, @Param("userId") int userId);

    /**
     * 获取下一章节顺序号
     */
    int getNextSortOrder(@Param("novelId") int novelId, @Param("userId") int userId);

    /**
     * 更新当前用户拥有的章节
     */
    int updateOwnedChapter(@Param("chapter") NovelChapter chapter, @Param("userId") int userId);

}
