package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.shiyq.entity.DO.NovelTag;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.mapper.NovelTagMapper;
import com.shiyq.mapper.NovelTagRelationMapper;
import com.shiyq.service.NovelTagService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class NovelTagServiceImpl extends ServiceImpl<NovelTagMapper, NovelTag> implements NovelTagService {

    private final NovelTagMapper tagMapper;
    private final NovelTagRelationMapper relationMapper;

    public NovelTagServiceImpl(NovelTagMapper tagMapper, NovelTagRelationMapper relationMapper) {
        this.tagMapper = tagMapper;
        this.relationMapper = relationMapper;
    }

    @Override
    public List<NovelTag> listAllTags() {
        int userId = UserContext.requireCurrentUserId();
        QueryWrapper<NovelTag> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByAsc("name");
        return tagMapper.selectList(wrapper);
    }

    @Override
    public List<String> assignTags(int novelId, List<String> tagNames) {
        int userId = UserContext.requireCurrentUserId();
        Set<String> normalizedNames = new LinkedHashSet<>();
        if (tagNames != null) {
            for (String tagName : tagNames) {
                if (tagName == null) {
                    continue;
                }
                String normalized = tagName.trim();
                if (!normalized.isEmpty()) {
                    if (normalized.length() > 32) {
                        throw new IllegalArgumentException("标签名不能超过32个字符");
                    }
                    normalizedNames.add(normalized);
                }
            }
        }

        for (String name : normalizedNames) {
            NovelTag tag = tagMapper.selectByName(name, userId);
            if (tag == null) {
                tag = new NovelTag();
                tag.setUserId(userId);
                tag.setName(name);
                try {
                    tagMapper.insert(tag);
                } catch (DuplicateKeyException ignored) {
                    tag = tagMapper.selectByName(name, userId);
                }
            }
            if (tag == null || relationMapper.insertRelation(novelId, tag.getId(), userId) != 1) {
                throw new IllegalStateException("保存小说标签失败");
            }
        }
        return new ArrayList<>(normalizedNames);
    }
}
