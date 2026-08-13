package com.shiyq.convert;

import com.shiyq.entity.DO.Novel;
import com.shiyq.entity.DO.NovelChapter;
import com.shiyq.entity.VO.NovelChapterVO;
import com.shiyq.entity.VO.NovelChapterDetailVO;
import com.shiyq.entity.VO.NovelVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface NovelConvert {
    NovelConvert INSTANCE = Mappers.getMapper(NovelConvert.class);

    /**
     * 小说DOList --> 小说VOList
     */
    @Mapping(target = "tags", ignore = true)
    List<NovelVO> toNovelVOList(List<Novel> list);

    /**
     * 小说DO --> 小说VO
     */
    @Mapping(source = "updateTime", target = "updateTime", dateFormat = "yyyy-MM-dd HH:mm")
    @Mapping(target = "tags", ignore = true)
    NovelVO toVO(Novel novel);

    /**
     * 小说章节DOList --> 小说章节VOList
     */
    List<NovelChapterVO> toChapterVOList(List<NovelChapter> list);

    /**
     * 小说章节DO --> 小说章节VO
     */
    NovelChapterVO toVO(NovelChapter chapter);

    /**
     * 小说章节DO --> 小说章节详情VO
     */
    @Mapping(source = "updateTime", target = "updateTime", dateFormat = "yyyy-MM-dd HH:mm")
    NovelChapterDetailVO toDetailVO(NovelChapter chapter);
}
