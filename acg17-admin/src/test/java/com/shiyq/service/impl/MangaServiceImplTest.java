package com.shiyq.service.impl;

import com.shiyq.entity.DO.Manga;
import com.shiyq.entity.DO.MangaTag;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.MangaDetailVO;
import com.shiyq.mapper.MangaMapper;
import com.shiyq.mapper.MangaTagRelationMapper;
import com.shiyq.service.MangaTagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MangaServiceImplTest {

    private static final int USER_ID = 31;

    @BeforeEach
    void setUp() {
        UserContext.add(USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void getMangaByIdAddsAccessUrlToPathsInCompactJson() {
        Manga manga = new Manga();
        manga.setId(9);
        manga.setCover("9/cover.jpg");
        manga.setPages("[{\"chapter\":1,\"pagelist\":[{\"page\":1,\"path\":\"9/1/page.png\"}]}]");

        MangaMapper mangaMapper = mock(MangaMapper.class);
        when(mangaMapper.getMangaDetailById(9L, USER_ID)).thenReturn(manga);
        MangaTagService mangaTagService = mock(MangaTagService.class);
        when(mangaTagService.getTagsByMangaId(9)).thenReturn(Collections.emptyList());

        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);
        service.setMangaTagService(mangaTagService);
        ReflectionTestUtils.setField(service, "serverFileUrlPrefix", "/api/file/");
        ReflectionTestUtils.setField(service, "mangaFolder", "manga/");

        MangaDetailVO result = service.getMangaById(9L);

        List<?> pageList = (List<?>) result.getPages().get(0).get("pagelist");
        Map<?, ?> page = (Map<?, ?>) pageList.get(0);
        String path = (String) page.get("path");
        assertEquals("/api/file/manga/9/1/page.png", path);
    }

    @Test
    void tagRelationWritesIncludeTheAuthenticatedUser() {
        Manga manga = new Manga();
        manga.setId(9);
        MangaTag tag = new MangaTag();
        tag.setId(7);
        tag.setUserId(USER_ID);

        MangaMapper mangaMapper = mock(MangaMapper.class);
        when(mangaMapper.getOwnedMangaById(9L, USER_ID)).thenReturn(manga);
        MangaTagService mangaTagService = mock(MangaTagService.class);
        when(mangaTagService.getOwnedTagById(7)).thenReturn(tag);
        MangaTagRelationMapper relationMapper = mock(MangaTagRelationMapper.class);

        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);
        service.setMangaTagService(mangaTagService);
        service.setMangaTagRelationMapper(relationMapper);

        assertTrue(service.addTagToManga(9L, 7));
        assertTrue(service.removeTagFromManga(9L, 7));

        verify(relationMapper).insertRelation(9, 7, USER_ID);
        verify(relationMapper).deleteRelation(9, 7, USER_ID);
    }
}
