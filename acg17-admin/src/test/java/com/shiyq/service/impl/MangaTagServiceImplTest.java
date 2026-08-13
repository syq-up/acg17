package com.shiyq.service.impl;

import com.shiyq.entity.DO.MangaTag;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.mapper.MangaTagMapper;
import com.shiyq.mapper.MangaTagRelationMapper;
import com.shiyq.service.MangaTagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MangaTagServiceImplTest {

    private MangaTagMapper tagMapper;
    private MangaTagRelationMapper relationMapper;
    private MangaTagServiceImpl service;

    @BeforeEach
    void setUp() {
        tagMapper = mock(MangaTagMapper.class);
        relationMapper = mock(MangaTagRelationMapper.class);
        service = new MangaTagServiceImpl();
        service.setMangaTagMapper(tagMapper);
        service.setRelationMapper(relationMapper);
        UserContext.add(1);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void referencedTagCannotBeDeleted() {
        MangaTag tag = new MangaTag();
        tag.setId(7);
        tag.setUserId(1);
        when(tagMapper.selectOne(any())).thenReturn(tag);
        when(relationMapper.countByTagId(7, 1)).thenReturn(2L);

        assertEquals(MangaTagService.DeleteResult.IN_USE, service.deleteUnusedTag(7));
        verify(tagMapper, never()).deleteById(7);
    }

    @Test
    void unusedTagCanBeDeleted() {
        MangaTag tag = new MangaTag();
        tag.setId(8);
        tag.setUserId(1);
        when(tagMapper.selectOne(any())).thenReturn(tag);
        when(relationMapper.countByTagId(8, 1)).thenReturn(0L);
        when(tagMapper.deleteById(8)).thenReturn(1);

        assertEquals(MangaTagService.DeleteResult.DELETED, service.deleteUnusedTag(8));
    }

    @Test
    void missingTagReturnsNotFound() {
        when(tagMapper.selectOne(any())).thenReturn(null);

        assertEquals(MangaTagService.DeleteResult.NOT_FOUND, service.deleteUnusedTag(9));
        verify(relationMapper, never()).countByTagId(9, 1);
    }

    @Test
    void anotherUsersTagIsNotVisibleOrDeletable() {
        when(tagMapper.selectOne(any())).thenReturn(null);

        assertNull(service.getOwnedTagById(10));
        assertEquals(MangaTagService.DeleteResult.NOT_FOUND, service.deleteUnusedTag(10));
        verify(relationMapper, never()).countByTagId(10, 1);
        verify(tagMapper, never()).deleteById(10);
    }

    @Test
    void tagCountsUseCurrentUserAndRequestedRecycleState() {
        when(tagMapper.listWithCounts(1, null, true))
                .thenReturn(Collections.emptyList());

        service.listTags(true);

        verify(tagMapper).listWithCounts(1, null, true);
    }
}
