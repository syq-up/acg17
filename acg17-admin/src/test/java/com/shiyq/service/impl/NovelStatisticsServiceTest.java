package com.shiyq.service.impl;

import com.shiyq.entity.DO.Novel;
import com.shiyq.entity.DO.NovelChapter;
import com.shiyq.entity.DTO.NovelCreateDTO;
import com.shiyq.entity.DTO.NovelChapterCreateDTO;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.mapper.NovelMapper;
import com.shiyq.mapper.NovelChapterMapper;
import com.shiyq.service.NovelTagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;

class NovelStatisticsServiceTest {

    private static final int USER_ID = 17;

    @AfterEach
    void clearUserContext() {
        UserContext.remove();
    }

    @Test
    void newNovelAlwaysStartsWithZeroWords() {
        NovelMapper novelMapper = mock(NovelMapper.class);
        NovelTagService novelTagService = mock(NovelTagService.class);
        when(novelMapper.insert(org.mockito.ArgumentMatchers.any(Novel.class))).thenAnswer(invocation -> {
            Novel novel = invocation.getArgument(0);
            novel.setId(100);
            return 1;
        });
        when(novelTagService.assignTags(100, Collections.emptyList())).thenReturn(Collections.emptyList());

        NovelServiceImpl service = new NovelServiceImpl();
        service.setNovelMapper(novelMapper);
        service.setNovelTagService(novelTagService);

        NovelCreateDTO request = new NovelCreateDTO();
        request.setTitle("test novel");
        request.setTags(Collections.emptyList());
        UserContext.add(USER_ID);
        service.addNovel(request);

    }

    @Test
    void contentNormalizationUsesTheSharedWordCountingRule() {
        NovelChapterServiceImpl service = new NovelChapterServiceImpl();
        NovelChapter chapter = new NovelChapter();
        chapter.setContent(Arrays.asList(" 你 好 ", null, "😀！"));

        service.normalizeContent(chapter);

        assertEquals(Arrays.asList("你 好", "😀！"), chapter.getContent());
        assertEquals(4, chapter.getTotalWords());
    }

    @Test
    void addingChapterLocksItsNovelBeforeAllocatingTheNextSortOrder() {
        NovelMapper novelMapper = mock(NovelMapper.class);
        NovelChapterMapper chapterMapper = mock(NovelChapterMapper.class);
        Novel novel = new Novel();
        novel.setId(9);
        when(novelMapper.selectOwnedByIdForUpdate(9, USER_ID)).thenReturn(novel);
        when(chapterMapper.getNextSortOrder(9, USER_ID)).thenReturn(4);
        when(chapterMapper.insert(org.mockito.ArgumentMatchers.any(NovelChapter.class))).thenReturn(1);
        when(novelMapper.updateTotalWordsByIncrease(
                org.mockito.ArgumentMatchers.eq(9),
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(1);

        NovelChapterServiceImpl service = new NovelChapterServiceImpl();
        service.setNovelMapper(novelMapper);
        service.setChapterMapper(chapterMapper);
        NovelChapterCreateDTO request = new NovelChapterCreateDTO();
        request.setNovelId(9);
        request.setTitle("chapter");
        request.setContent(Collections.singletonList("content"));
        UserContext.add(USER_ID);

        assertTrue(service.addChapter(request));

        InOrder inOrder = inOrder(novelMapper, chapterMapper);
        inOrder.verify(novelMapper).selectOwnedByIdForUpdate(9, USER_ID);
        inOrder.verify(chapterMapper).getNextSortOrder(9, USER_ID);
        inOrder.verify(chapterMapper).insert(org.mockito.ArgumentMatchers.any(NovelChapter.class));
    }
}
