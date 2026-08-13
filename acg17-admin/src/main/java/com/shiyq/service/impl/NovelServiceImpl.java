package com.shiyq.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.shiyq.convert.NovelConvert;
import com.shiyq.entity.DO.Novel;
import com.shiyq.entity.DTO.NovelCreateDTO;
import com.shiyq.entity.DTO.NovelTagAssignment;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.NovelVO;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.mapper.NovelMapper;
import com.shiyq.mapper.NovelTagMapper;
import com.shiyq.service.NovelService;
import com.shiyq.service.NovelTagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NovelServiceImpl extends ServiceImpl<NovelMapper, Novel> implements NovelService {

    private NovelMapper novelMapper;
    private NovelTagMapper novelTagMapper;
    private NovelTagService novelTagService;

    @Autowired
    public void setNovelMapper(NovelMapper novelMapper) {
        this.novelMapper = novelMapper;
    }

    @Autowired
    public void setNovelTagMapper(NovelTagMapper novelTagMapper) {
        this.novelTagMapper = novelTagMapper;
    }

    @Autowired
    public void setNovelTagService(NovelTagService novelTagService) {
        this.novelTagService = novelTagService;
    }

    @Override
    public PageVO<NovelVO> getList(long pageNum, boolean deleted, Integer tagId, String keyword) {
        int userId = UserContext.requireCurrentUserId();
        PageVO<NovelVO> pageVO = new PageVO<>(30L, pageNum);
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }
        List<Novel> novels = novelMapper.getListByCondition(
                userId, pageNum, 30L, deleted, tagId, normalizedKeyword);
        List<NovelVO> records = NovelConvert.INSTANCE.toNovelVOList(novels);
        attachTags(novels, records, userId);
        pageVO.setRecords(records);
        pageVO.setTotal(novelMapper.getTotalByCondition(userId, deleted, tagId, normalizedKeyword));
        return pageVO;
    }

    @Override
    public NovelVO getNovelById(int id) {
        int userId = UserContext.requireCurrentUserId();
        Novel novel = novelMapper.selectOwnedById(id, userId);
        if (novel == null) {
            return null;
        }
        NovelVO vo = NovelConvert.INSTANCE.toVO(novel);
        attachTags(Collections.singletonList(novel), Collections.singletonList(vo), userId);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NovelVO addNovel(NovelCreateDTO request) {
        if (request == null || request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("书名不能为空");
        }
        String title = request.getTitle().trim();
        if (title.length() > 100) {
            throw new IllegalArgumentException("书名不能超过100个字符");
        }

        int userId = UserContext.requireCurrentUserId();
        Novel novel = new Novel();
        novel.setUserId(userId);
        novel.setTitle(title);
        String author = request.getAuthor() == null ? null : request.getAuthor().trim();
        if (author != null && author.length() > 100) {
            throw new IllegalArgumentException("作者名不能超过100个字符");
        }
        novel.setAuthor(author == null || author.isEmpty() ? null : author);
        novel.setTotalWords(0);
        novel.setDeleted(false);
        if (novelMapper.insert(novel) != 1) {
            throw new IllegalStateException("新增小说失败");
        }
        List<String> tags = novelTagService.assignTags(novel.getId(), request.getTags());
        NovelVO vo = NovelConvert.INSTANCE.toVO(novel);
        vo.setTags(tags);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNovelById(int id) {
        int userId = UserContext.requireCurrentUserId();
        Novel novel = novelMapper.selectOwnedById(id, userId);
        if (novel == null || novelMapper.deleteByIdAndUserId(id, userId) <= 0) {
            return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreNovelById(int id) {
        return novelMapper.restoreByIdAndUserId(id, UserContext.requireCurrentUserId()) == 1;
    }

    private void attachTags(List<Novel> novels, List<NovelVO> records, int userId) {
        if (novels.isEmpty()) {
            return;
        }
        List<Integer> novelIds = novels.stream().map(Novel::getId).collect(Collectors.toList());
        Map<Integer, List<String>> tagsByNovelId = new HashMap<>();
        for (NovelTagAssignment assignment : novelTagMapper.listAssignmentsByNovelIds(novelIds, userId)) {
            tagsByNovelId.computeIfAbsent(assignment.getNovelId(), ignored -> new ArrayList<>())
                    .add(assignment.getName());
        }
        for (int i = 0; i < novels.size(); i++) {
            records.get(i).setTags(tagsByNovelId.getOrDefault(novels.get(i).getId(), Collections.emptyList()));
        }
    }
}
