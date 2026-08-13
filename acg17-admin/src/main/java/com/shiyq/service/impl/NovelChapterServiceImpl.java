package com.shiyq.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.shiyq.convert.NovelConvert;
import com.shiyq.entity.DO.Novel;
import com.shiyq.entity.DO.NovelChapter;
import com.shiyq.entity.DTO.NovelChapterCreateDTO;
import com.shiyq.entity.DTO.NovelChapterUpdateDTO;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.NovelChapterDetailVO;
import com.shiyq.entity.VO.NovelChapterVO;
import com.shiyq.mapper.NovelChapterMapper;
import com.shiyq.mapper.NovelMapper;
import com.shiyq.service.NovelChapterService;
import com.shiyq.util.NovelWordCountUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class NovelChapterServiceImpl extends ServiceImpl<NovelChapterMapper, NovelChapter>
        implements NovelChapterService {

    private static final int MAX_PARAGRAPHS = 10000;
    private static final int MAX_PARAGRAPH_LENGTH = 100000;
    private static final long MAX_CONTENT_LENGTH = 2_000_000L;

    private NovelMapper novelMapper;
    private NovelChapterMapper chapterMapper;

    @Autowired
    public void setNovelMapper(NovelMapper novelMapper) {
        this.novelMapper = novelMapper;
    }

    @Autowired
    public void setChapterMapper(NovelChapterMapper chapterMapper) {
        this.chapterMapper = chapterMapper;
    }

    @Override
    public List<NovelChapterVO> getList(int novelId) {
        return NovelConvert.INSTANCE.toChapterVOList(
                chapterMapper.getList(novelId, UserContext.requireCurrentUserId()));
    }

    @Override
    public NovelChapterDetailVO getContentById(int id) {
        NovelChapter chapter = chapterMapper.selectOwnedById(id, UserContext.requireCurrentUserId());
        return chapter == null ? null : NovelConvert.INSTANCE.toDetailVO(chapter);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addChapter(NovelChapterCreateDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("章节信息不能为空");
        }
        if (request.getNovelId() == null || request.getNovelId() <= 0) {
            throw new IllegalArgumentException("小说ID必须大于0");
        }
        int userId = UserContext.requireCurrentUserId();
        int novelId = request.getNovelId();
        Novel novel = novelMapper.selectOwnedByIdForUpdate(novelId, userId);
        if (novel == null) {
            return false;
        }

        NovelChapter chapter = new NovelChapter();
        chapter.setNovelId(novelId);
        chapter.setTitle(normalizeTitle(request.getTitle()));
        chapter.setContent(request.getContent());
        normalizeContent(chapter);
        chapter.setSortOrder(chapterMapper.getNextSortOrder(novelId, userId));
        if (chapterMapper.insert(chapter) != 1) {
            throw new IllegalStateException("新增章节失败");
        }
        updateWordStatistics(novelId, userId, chapter.getTotalWords());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateChapter(NovelChapterUpdateDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("章节信息不能为空");
        }
        if (request.getId() == null || request.getId() <= 0) {
            throw new IllegalArgumentException("章节ID必须大于0");
        }
        int userId = UserContext.requireCurrentUserId();
        int chapterId = request.getId();
        NovelChapter oldChapter = chapterMapper.selectOwnedById(chapterId, userId);
        if (oldChapter == null) {
            return false;
        }

        NovelChapter chapter = new NovelChapter();
        chapter.setId(chapterId);
        chapter.setTitle(normalizeTitle(request.getTitle()));
        chapter.setContent(request.getContent());
        normalizeContent(chapter);

        boolean changed = !Objects.equals(oldChapter.getTitle(), chapter.getTitle())
                || !Objects.equals(oldChapter.getContent(), chapter.getContent());
        if (!changed) {
            return true;
        }
        if (chapterMapper.updateOwnedChapter(chapter, userId) != 1) {
            throw new IllegalStateException("更新章节失败");
        }

        int oldTotalWords = oldChapter.getTotalWords() == null ? 0 : oldChapter.getTotalWords();
        int increase = chapter.getTotalWords() - oldTotalWords;
        if (increase != 0) {
            updateWordStatistics(oldChapter.getNovelId(), userId, increase);
        }
        return true;
    }

    public NovelChapter normalizeContent(NovelChapter chapter) {
        List<String> contentList = chapter.getContent();
        if (contentList == null || contentList.isEmpty()) {
            chapter.setContent(new ArrayList<>());
            chapter.setTotalWords(0);
            return chapter;
        }
        if (contentList.size() > MAX_PARAGRAPHS) {
            throw new IllegalArgumentException("章节段落不能超过10000段");
        }
        long totalLength = 0L;
        for (String paragraph : contentList) {
            if (paragraph == null) {
                throw new IllegalArgumentException("章节段落不能为空");
            }
            if (paragraph.length() > MAX_PARAGRAPH_LENGTH) {
                throw new IllegalArgumentException("单个章节段落不能超过100000个字符");
            }
            totalLength += paragraph.length();
            if (totalLength > MAX_CONTENT_LENGTH) {
                throw new IllegalArgumentException("章节内容不能超过2000000个字符");
            }
        }

        List<String> normalized = new ArrayList<>();
        if (contentList.size() == 1 && contentList.get(0) != null && contentList.get(0).contains("\n")) {
            String[] paragraphs = contentList.get(0).split("\\r?\\n\\s*\\r?\\n");
            appendNormalizedParagraphs(normalized, paragraphs);
        } else {
            appendNormalizedParagraphs(normalized, contentList.toArray(new String[0]));
        }
        chapter.setContent(normalized);
        chapter.setTotalWords(NovelWordCountUtil.count(normalized));
        return chapter;
    }

    private void appendNormalizedParagraphs(List<String> target, String[] paragraphs) {
        for (String paragraph : paragraphs) {
            if (paragraph == null) {
                continue;
            }
            String normalized = paragraph.trim()
                    .replaceAll("^[\\s　]+", "")
                    .replaceAll("[\\s　]+$", "");
            if (!normalized.isEmpty()) {
                target.add(normalized);
                if (target.size() > MAX_PARAGRAPHS) {
                    throw new IllegalArgumentException("章节段落不能超过10000段");
                }
            }
        }
    }

    private String normalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("章节名不能为空");
        }
        String normalized = title.trim();
        if (normalized.length() > 150) {
            throw new IllegalArgumentException("章节名不能超过150个字符");
        }
        return normalized;
    }

    private void updateWordStatistics(int novelId, int userId, int increase) {
        if (novelMapper.updateTotalWordsByIncrease(novelId, userId, increase) != 1) {
            throw new IllegalStateException("更新小说字数失败");
        }
    }
}
