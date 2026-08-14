package com.shiyq.service.impl;

import com.shiyq.entity.DO.MangaTag;
import com.shiyq.mapper.MangaTagMapper;
import com.shiyq.mapper.MangaTagRelationMapper;
import com.shiyq.service.MangaTagService;
import com.shiyq.entity.DTO.UserContext;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.shiyq.entity.VO.MangaTagListVO;
import com.shiyq.entity.VO.MangaTagVO;
import com.shiyq.constant.MangaConstant;
/**
 * <p>
 * 漫画标签表 服务实现类
 * </p>
 *
 * @author shiyq
 * @since 2024-01-20
 */
@Service
public class MangaTagServiceImpl extends ServiceImpl<MangaTagMapper, MangaTag> implements MangaTagService {

    private MangaTagMapper mangaTagMapper;
    private MangaTagRelationMapper relationMapper;

    @Autowired
    public void setMangaTagMapper(MangaTagMapper mangaTagMapper) {
        this.mangaTagMapper = mangaTagMapper;
    }

    @Autowired
    public void setRelationMapper(MangaTagRelationMapper relationMapper) {
        this.relationMapper = relationMapper;
    }

    @Override
    public MangaTagListVO listTags(boolean deleted) {
        int userId = UserContext.requireCurrentUserId();
        List<MangaTagVO> allTags = mangaTagMapper.listWithCounts(
                userId, null, deleted);
        MangaTagListVO mangaTagListVO = new MangaTagListVO();
        mangaTagListVO.setCharacterTags(
            allTags.stream().filter(tag -> MangaConstant.TAG_CATEGORY_CHARACTER.equals(tag.getCategory())).collect(Collectors.toList()))
                .setMaleTags(
            allTags.stream().filter(tag -> MangaConstant.TAG_CATEGORY_MALE.equals(tag.getCategory())).collect(Collectors.toList()))
                .setFemaleTags(
            allTags.stream().filter(tag -> MangaConstant.TAG_CATEGORY_FEMALE.equals(tag.getCategory())).collect(Collectors.toList()))
                .setMixedTags(
            allTags.stream().filter(tag -> MangaConstant.TAG_CATEGORY_MIXED.equals(tag.getCategory())).collect(Collectors.toList()))
                .setOtherTags(
            allTags.stream().filter(tag -> MangaConstant.TAG_CATEGORY_OTHER.equals(tag.getCategory())).collect(Collectors.toList()))
                .setOriginalTags(
            allTags.stream().filter(tag -> MangaConstant.TAG_CATEGORY_ORIGINAL.equals(tag.getCategory())).collect(Collectors.toList()))
                .setArtistTags(
            allTags.stream().filter(tag -> MangaConstant.TAG_CATEGORY_ARTIST.equals(tag.getCategory())).collect(Collectors.toList()))
                .setGroupTags(
            allTags.stream().filter(tag -> MangaConstant.TAG_CATEGORY_GROUP.equals(tag.getCategory())).collect(Collectors.toList()));
        return mangaTagListVO;
    }

    @Override
    public List<MangaTagVO> getTagsByCategory(Integer category) {
        return mangaTagMapper.listWithCounts(
                UserContext.requireCurrentUserId(), category, false);
    }

    @Override
    public List<MangaTagVO> getTagsByMangaId(Integer mangaId) {
        return mangaTagMapper.listByMangaIdWithCounts(
                mangaId, UserContext.requireCurrentUserId());
    }

    @Override
    public MangaTag getOwnedTagById(Integer tagId) {
        if (tagId == null) {
            return null;
        }
        int userId = UserContext.requireCurrentUserId();
        QueryWrapper<MangaTag> wrapper = new QueryWrapper<>();
        wrapper.eq("id", tagId).eq("user_id", userId);
        return mangaTagMapper.selectOne(wrapper);
    }

    @Override
    public MangaTag getOrCreateTagByNameAndCategory(String tagName, Integer category) {
        int userId = UserContext.requireCurrentUserId();
        String normalizedName = tagName == null ? "" : tagName.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        if (normalizedName.length() > 50) {
            throw new IllegalArgumentException("标签名称不能超过50个字符");
        }
        if (!MangaConstant.isValidCategory(category)) {
            throw new IllegalArgumentException("标签分类无效");
        }
        QueryWrapper<MangaTag> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("tag_name", normalizedName)
                .eq("category", category);
        MangaTag existingTag = this.getOne(queryWrapper);
        if (existingTag != null) {
            return existingTag;
        }

        MangaTag newTag = new MangaTag();
        newTag.setUserId(userId);
        newTag.setTagName(normalizedName);
        newTag.setCategory(category);
        try {
            this.save(newTag);
            return newTag;
        } catch (DuplicateKeyException exception) {
            return this.getOne(queryWrapper);
        }
    }

    @Override
    public boolean updateOwnedTag(Integer tagId, String tagName, Integer category) {
        if (tagId == null || tagId <= 0) {
            return false;
        }
        String normalizedName = tagName == null ? "" : tagName.trim();
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("标签名称不能为空");
        }
        if (normalizedName.length() > 50) {
            throw new IllegalArgumentException("标签名称不能超过50个字符");
        }
        if (!MangaConstant.isValidCategory(category)) {
            throw new IllegalArgumentException("标签分类无效");
        }

        MangaTag update = new MangaTag();
        update.setTagName(normalizedName);
        update.setCategory(category);
        LambdaUpdateWrapper<MangaTag> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(MangaTag::getId, tagId)
                .eq(MangaTag::getUserId, UserContext.requireCurrentUserId());
        try {
            return mangaTagMapper.update(update, wrapper) == 1;
        } catch (DuplicateKeyException exception) {
            throw new IllegalArgumentException("当前分类下已存在同名标签", exception);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResult deleteUnusedTag(Integer tagId) {
        MangaTag tag = getOwnedTagById(tagId);
        if (tag == null) {
            return DeleteResult.NOT_FOUND;
        }
        if (relationMapper.countByTagId(tagId, UserContext.requireCurrentUserId()) > 0) {
            return DeleteResult.IN_USE;
        }
        try {
            return mangaTagMapper.deleteById(tagId) == 1
                    ? DeleteResult.DELETED
                    : DeleteResult.NOT_FOUND;
        } catch (DataIntegrityViolationException exception) {
            // 引用可能在计数后并发创建，最终仍由外键兜底。
            return DeleteResult.IN_USE;
        }
    }

}
