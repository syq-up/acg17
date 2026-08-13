package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.shiyq.entity.DTO.NovelChapterCreateDTO;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.mapper.GameMapper;
import com.shiyq.mapper.IllustrationMapper;
import com.shiyq.mapper.MangaMapper;
import com.shiyq.mapper.NovelChapterMapper;
import com.shiyq.mapper.NovelMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ContentOwnershipServiceTest {

    private static final int USER_A = 11;
    private static final int USER_B = 22;

    @AfterEach
    void clearUserContext() {
        UserContext.remove();
    }

    @Test
    void sameResourceIdUsesTheAuthenticatedUserForPrivateReads() {
        GameMapper gameMapper = mock(GameMapper.class);
        GameServiceImpl gameService = new GameServiceImpl();
        gameService.setGameMapper(gameMapper);

        MangaMapper mangaMapper = mock(MangaMapper.class);
        MangaServiceImpl mangaService = new MangaServiceImpl();
        mangaService.setMangaMapper(mangaMapper);

        NovelMapper novelMapper = mock(NovelMapper.class);
        NovelServiceImpl novelService = new NovelServiceImpl();
        novelService.setNovelMapper(novelMapper);

        NovelChapterMapper chapterMapper = mock(NovelChapterMapper.class);
        NovelChapterServiceImpl chapterService = new NovelChapterServiceImpl();
        chapterService.setChapterMapper(chapterMapper);

        UserContext.add(USER_A);
        assertNull(gameService.getGameById(7));
        assertNull(mangaService.getMangaById(7));
        assertNull(novelService.getNovelById(7));
        assertNull(chapterService.getContentById(7));

        UserContext.add(USER_B);
        assertNull(gameService.getGameById(7));
        assertNull(mangaService.getMangaById(7));
        assertNull(novelService.getNovelById(7));
        assertNull(chapterService.getContentById(7));

        verify(gameMapper).selectByIdAndUserId(7, USER_A);
        verify(gameMapper).selectByIdAndUserId(7, USER_B);
        verify(mangaMapper).getMangaDetailById(7L, USER_A);
        verify(mangaMapper).getMangaDetailById(7L, USER_B);
        verify(novelMapper).selectOwnedById(7, USER_A);
        verify(novelMapper).selectOwnedById(7, USER_B);
        verify(chapterMapper).selectOwnedById(7, USER_A);
        verify(chapterMapper).selectOwnedById(7, USER_B);
    }

    @Test
    void privateWritesAlwaysUseTheAuthenticatedUser() {
        IllustrationMapper illustrationMapper = mock(IllustrationMapper.class);
        when(illustrationMapper.deleteByIdAndUserId(9, USER_A)).thenReturn(1);
        IllustrationServiceImpl illustrationService = new IllustrationServiceImpl();
        illustrationService.setIllustrationMapper(illustrationMapper);

        GameMapper gameMapper = mock(GameMapper.class);
        when(gameMapper.deleteByIdAndUserId(9, USER_A)).thenReturn(1);
        GameServiceImpl gameService = new GameServiceImpl();
        gameService.setGameMapper(gameMapper);

        MangaMapper mangaMapper = mock(MangaMapper.class);
        when(mangaMapper.deleteMangaByIdAndUserId(9L, USER_A)).thenReturn(1);
        MangaServiceImpl mangaService = new MangaServiceImpl();
        mangaService.setMangaMapper(mangaMapper);

        NovelMapper novelMapper = mock(NovelMapper.class);
        when(novelMapper.restoreByIdAndUserId(9, USER_A)).thenReturn(1);
        NovelServiceImpl novelService = new NovelServiceImpl();
        novelService.setNovelMapper(novelMapper);

        UserContext.add(USER_A);
        assertTrue(illustrationService.deleteById(9));
        assertTrue(gameService.deleteGame(9));
        assertTrue(mangaService.deleteMangaById(9));
        assertTrue(novelService.restoreNovelById(9));

        UserContext.add(USER_B);
        assertFalse(illustrationService.deleteById(9));
        assertFalse(gameService.deleteGame(9));
        assertFalse(mangaService.deleteMangaById(9));
        assertFalse(novelService.restoreNovelById(9));

        verify(illustrationMapper).deleteByIdAndUserId(9, USER_A);
        verify(illustrationMapper).deleteByIdAndUserId(9, USER_B);
        verify(gameMapper).deleteByIdAndUserId(9, USER_A);
        verify(gameMapper).deleteByIdAndUserId(9, USER_B);
        verify(mangaMapper).deleteMangaByIdAndUserId(9L, USER_A);
        verify(mangaMapper).deleteMangaByIdAndUserId(9L, USER_B);
        verify(novelMapper).restoreByIdAndUserId(9, USER_A);
        verify(novelMapper).restoreByIdAndUserId(9, USER_B);
    }

    @Test
    void novelChildOperationsRequireOwnershipOfTheParentNovel() {
        NovelMapper novelMapper = mock(NovelMapper.class);
        NovelChapterMapper chapterMapper = mock(NovelChapterMapper.class);
        NovelChapterServiceImpl chapterService = new NovelChapterServiceImpl();
        chapterService.setNovelMapper(novelMapper);
        chapterService.setChapterMapper(chapterMapper);

        NovelChapterCreateDTO request = new NovelChapterCreateDTO();
        request.setNovelId(99);
        request.setTitle("foreign chapter");
        request.setContent(Collections.singletonList("content"));

        UserContext.add(USER_B);
        when(novelMapper.selectOwnedByIdForUpdate(99, USER_B)).thenReturn(null);

        assertFalse(chapterService.addChapter(request));
        verify(novelMapper).selectOwnedByIdForUpdate(99, USER_B);
        verifyNoInteractions(chapterMapper);
    }

    @Test
    void chapterListsAreScopedToEachAuthenticatedUser() {
        NovelChapterMapper chapterMapper = mock(NovelChapterMapper.class);
        when(chapterMapper.getList(5, USER_A)).thenReturn(Collections.emptyList());
        when(chapterMapper.getList(5, USER_B)).thenReturn(Collections.emptyList());
        NovelChapterServiceImpl chapterService = new NovelChapterServiceImpl();
        chapterService.setChapterMapper(chapterMapper);

        UserContext.add(USER_A);
        chapterService.getList(5);
        UserContext.add(USER_B);
        chapterService.getList(5);

        verify(chapterMapper).getList(5, USER_A);
        verify(chapterMapper).getList(5, USER_B);
    }

    @Test
    void missingUserContextFailsClosedBeforeQueryingPrivateData() {
        NovelMapper novelMapper = mock(NovelMapper.class);
        NovelServiceImpl novelService = new NovelServiceImpl();
        novelService.setNovelMapper(novelMapper);

        assertThrows(IllegalStateException.class, () -> novelService.getNovelById(3));
        verifyNoInteractions(novelMapper);
    }

    @Test
    void ownershipMapperXmlFilesCanBeParsed() throws Exception {
        String[] resources = {
                "mapper/GameMapper.xml",
                "mapper/IllustrationMapper.xml",
                "mapper/MangaMapper.xml",
                "mapper/MangaTagMapper.xml",
                "mapper/MangaTagRelationMapper.xml",
                "mapper/NovelMapper.xml",
                "mapper/NovelChapterMapper.xml",
                "mapper/NovelTagMapper.xml",
                "mapper/NovelTagRelationMapper.xml",
                "mapper/UserMapper.xml",
                "mapper/UserInfoMapper.xml"
        };
        MybatisConfiguration configuration = new MybatisConfiguration();
        for (String resource : resources) {
            try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
                XMLMapperBuilder parser = new XMLMapperBuilder(
                        inputStream, configuration, resource, configuration.getSqlFragments());
                parser.parse();
            }
        }
    }
}
