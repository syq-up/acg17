package com.shiyq.mapper;

import com.shiyq.entity.DO.MangaTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyq.entity.VO.MangaTagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 漫画标签表 Mapper 接口
 * </p>
 *
 * @author shiyq
 * @since 2024-01-20
 */
@Mapper
public interface MangaTagMapper extends BaseMapper<MangaTag> {

    List<MangaTagVO> listWithCounts(@Param("userId") Integer userId,
                                    @Param("category") Integer category,
                                    @Param("deleted") Boolean deleted);

    List<MangaTagVO> listByMangaIdWithCounts(@Param("mangaId") Integer mangaId,
                                             @Param("userId") Integer userId);
}
