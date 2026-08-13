package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiyq.entity.DO.NovelTag;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.mapper.NovelTagMapper;
import com.shiyq.mapper.NovelTagRelationMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NovelTagServiceImplTest {

    private static final int USER_ID = 31;

    private NovelTagMapper tagMapper;
    private NovelTagRelationMapper relationMapper;
    private NovelTagServiceImpl service;

    @BeforeEach
    void setUp() {
        tagMapper = mock(NovelTagMapper.class);
        relationMapper = mock(NovelTagRelationMapper.class);
        service = new NovelTagServiceImpl(tagMapper, relationMapper);
        UserContext.add(USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void tagListIsScopedToTheAuthenticatedUser() {
        when(tagMapper.selectList(any())).thenReturn(Collections.emptyList());

        service.listAllTags();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<NovelTag>> wrapperCaptor =
                ArgumentCaptor.forClass(QueryWrapper.class);
        verify(tagMapper).selectList(wrapperCaptor.capture());
        QueryWrapper<NovelTag> wrapper = wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().contains("user_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(USER_ID));
    }

    @Test
    void existingTagLookupAndRelationWriteUseTheAuthenticatedUser() {
        NovelTag tag = new NovelTag();
        tag.setId(7);
        tag.setUserId(USER_ID);
        tag.setName("fantasy");
        when(tagMapper.selectByName("fantasy", USER_ID)).thenReturn(tag);
        when(relationMapper.insertRelation(9, 7, USER_ID)).thenReturn(1);

        assertEquals(Collections.singletonList("fantasy"),
                service.assignTags(9, Collections.singletonList(" fantasy ")));

        verify(tagMapper).selectByName("fantasy", USER_ID);
        verify(relationMapper).insertRelation(9, 7, USER_ID);
    }

    @Test
    void newTagIsOwnedByTheAuthenticatedUser() {
        when(tagMapper.selectByName("new tag", USER_ID)).thenReturn(null);
        when(tagMapper.insert(any(NovelTag.class))).thenAnswer(invocation -> {
            NovelTag tag = invocation.getArgument(0);
            tag.setId(8);
            return 1;
        });
        when(relationMapper.insertRelation(9, 8, USER_ID)).thenReturn(1);

        service.assignTags(9, Collections.singletonList("new tag"));

        ArgumentCaptor<NovelTag> tagCaptor = ArgumentCaptor.forClass(NovelTag.class);
        verify(tagMapper).insert(tagCaptor.capture());
        assertEquals(USER_ID, tagCaptor.getValue().getUserId());
        verify(relationMapper).insertRelation(9, 8, USER_ID);
    }

    @Test
    void rejectsBlankTagsAndMoreThanThirtyTags() {
        assertThrows(IllegalArgumentException.class,
                () -> service.assignTags(9, Collections.singletonList("  ")));

        ArrayList<String> tooManyTags = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            tooManyTags.add("tag-" + i);
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.assignTags(9, tooManyTags));
    }
}
