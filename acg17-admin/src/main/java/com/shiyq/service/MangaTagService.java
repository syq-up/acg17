package com.shiyq.service;

import com.shiyq.entity.DO.MangaTag;

import java.util.List;
import com.shiyq.entity.VO.MangaTagListVO;
import com.shiyq.entity.VO.MangaTagVO;

/**
 * <p>
 * 漫画标签表 服务类
 * </p>
 *
 * @author shiyq
 * @since 2024-01-20
 */
public interface MangaTagService {

    enum DeleteResult {
        DELETED,
        NOT_FOUND,
        IN_USE
    }

    /**
     * 获取所有标签并返回分类VO
     * @param deleted 是否统计回收站中的漫画
     * @return 标签分类列表VO
     */
    MangaTagListVO listTags(boolean deleted);

    /**
     * 根据分类获取标签列表
     * @param category 分类标记（1-角色，2-男性，3-女性，4-混合，5-其他，6-原作）
     * @return 标签列表
     */
    List<MangaTagVO> getTagsByCategory(Integer category);

    /**
     * 获取指定漫画的标签及实时引用数。
     */
    List<MangaTagVO> getTagsByMangaId(Integer mangaId);

    /**
     * 获取当前用户拥有的标签。
     */
    MangaTag getOwnedTagById(Integer tagId);

    /**
     * 根据标签名和分类获取或创建标签
     * @param tagName 标签名
     * @param category 分类标记
     * @return 标签对象
     */
    MangaTag getOrCreateTagByNameAndCategory(String tagName, Integer category);

    /**
     * 更新当前用户拥有的标签。
     */
    boolean updateOwnedTag(MangaTag mangaTag);

    /**
     * 仅删除没有被漫画引用的标签。
     */
    DeleteResult deleteUnusedTag(Integer tagId);

}
