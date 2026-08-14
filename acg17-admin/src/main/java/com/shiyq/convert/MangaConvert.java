package com.shiyq.convert;

import com.shiyq.entity.DO.Manga;
import com.shiyq.entity.DO.MangaTag;
import com.shiyq.entity.VO.MangaVO;
import com.shiyq.entity.VO.MangaDetailVO;
import com.shiyq.entity.VO.MangaTagVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import java.util.List;

@Mapper
public interface MangaConvert {
    MangaConvert INSTANCE = Mappers.getMapper(MangaConvert.class);

    @Mapping(source = "id", target = "tagId")
    @Mapping(target = "tagCount", ignore = true)
    MangaTagVO toVO(MangaTag mangaTag);

    List<MangaTagVO> toVOList(List<MangaTag> mangaTags);

    /**
     * Manga转MangaVO
     * @param manga 漫画实体
     * @return 漫画VO
     */
    MangaVO toMangaVO(Manga manga);

    /**
     * Manga列表转MangaVO列表
     * @param mangaList 漫画实体列表
     * @return 漫画VO列表
     */
    List<MangaVO> toMangaVOList(List<Manga> mangaList);

    /**
     * Manga转MangaDetailVO
     * @param manga 漫画实体
     * @return 漫画详情VO
     */
    @Mapping(source = "updateTime", target = "updateTime", dateFormat = "yyyy-MM-dd HH:mm")
    @Mapping(target = "pages", ignore = true)
    @Mapping(target = "characterTags", ignore = true)
    @Mapping(target = "maleTags", ignore = true)
    @Mapping(target = "femaleTags", ignore = true)
    @Mapping(target = "mixedTags", ignore = true)
    @Mapping(target = "otherTags", ignore = true)
    @Mapping(target = "originalTags", ignore = true)
    @Mapping(target = "artistTags", ignore = true)
    @Mapping(target = "groupTags", ignore = true)
    MangaDetailVO toMangaDetailVO(Manga manga);

}
