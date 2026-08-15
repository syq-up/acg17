package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.shiyq.entity.DO.Manga;
import com.shiyq.entity.DO.MangaTag;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.DTO.MangaUploadDTO;
import com.shiyq.entity.VO.MangaDetailVO;
import com.shiyq.mapper.MangaMapper;
import com.shiyq.mapper.MangaTagRelationMapper;
import com.shiyq.service.MangaTagService;
import com.shiyq.service.MediaUrlSigner;
import com.shiyq.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
        MediaUrlSigner mediaUrlSigner = mock(MediaUrlSigner.class);
        when(mediaUrlSigner.sign("manga/", "9/cover.jpg")).thenReturn("/api/media?signed=cover");
        when(mediaUrlSigner.sign("manga/", "9/1/page.png")).thenReturn("/api/media?signed=page");

        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);
        service.setMangaTagService(mangaTagService);
        service.setMediaUrlSigner(mediaUrlSigner);
        service.setObjectMapper(new ObjectMapper());
        ReflectionTestUtils.setField(service, "mangaFolder", "manga/");

        MangaDetailVO result = service.getMangaById(9L);

        String path = result.getPages().get(0).getPagelist().get(0).getPath();
        assertEquals("/api/media?signed=page", path);
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
        service.setObjectMapper(new ObjectMapper());

        assertTrue(service.addTagToManga(9L, 7));
        assertTrue(service.removeTagFromManga(9L, 7));

        verify(relationMapper).insertRelation(9, 7, USER_ID);
        verify(relationMapper).deleteRelation(9, 7, USER_ID);
    }

    @Test
    void mangaListNormalizesDuplicateTagIdsBeforeQuerying() {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        when(mangaMapper.getListByCondition(
                USER_ID, 1L, 30L, false, null, List.of(7, 8)))
                .thenReturn(Collections.emptyList());
        when(mangaMapper.getTotalByCondition(
                USER_ID, false, null, List.of(7, 8)))
                .thenReturn(0L);

        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);

        service.getList(1, false, null, List.of(7, 7, 8));

        verify(mangaMapper).getListByCondition(
                USER_ID, 1L, 30L, false, null, List.of(7, 8));
        verify(mangaMapper).getTotalByCondition(
                USER_ID, false, null, List.of(7, 8));
    }

    @Test
    void mangaListRejectsInvalidTagIdsBeforeQuerying() {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);

        assertThrows(IllegalArgumentException.class,
                () -> service.getList(1, false, null, Arrays.asList(7, null)));

        verifyNoInteractions(mangaMapper);
    }

    @Test
    void mangaListMapperBuildsAnAndFilterForAllSelectedTags() throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        String resource = "mapper/MangaMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(
                    inputStream, configuration, resource, configuration.getSqlFragments())
                    .parse();
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", USER_ID);
        parameters.put("pageNum", 1L);
        parameters.put("pageSize", 30L);
        parameters.put("deleted", false);
        parameters.put("title", null);
        parameters.put("tagIds", List.of(7, 8));

        BoundSql boundSql = configuration
                .getMappedStatement("com.shiyq.mapper.MangaMapper.getListByCondition")
                .getBoundSql(parameters);
        String sql = boundSql.getSql().replaceAll("\\s+", " ");
        List<String> parameterNames = boundSql.getParameterMappings().stream()
                .map(mapping -> mapping.getProperty())
                .toList();

        assertTrue(sql.contains("r.`tag_id` IN"));
        assertTrue(sql.contains("HAVING COUNT(DISTINCT r.`tag_id`) = ?"));
        assertTrue(parameterNames.contains("__frch_tagId_0"));
        assertTrue(parameterNames.contains("__frch_tagId_1"));
    }

    @Test
    void mangaUploadRejectsMalformedTagJsonBeforeDatabaseWrite() {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);
        service.setFileStorageService(mock(FileStorageService.class));
        service.setObjectMapper(new ObjectMapper());
        MangaUploadDTO request = new MangaUploadDTO();
        request.setTitle("manga");
        request.setTags("{");
        request.setFile(new MockMultipartFile(
                "file", "manga.zip", "application/zip", new byte[] {1}));

        assertThrows(IllegalArgumentException.class, () -> service.addManga(request));

        verifyNoInteractions(mangaMapper);
    }
}
