package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.mapper.NovelMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class NovelServiceImplTest {

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
    void novelListPassesGlobalSortToThePagedQuery() {
        NovelMapper novelMapper = mock(NovelMapper.class);
        when(novelMapper.getListByCondition(
                USER_ID, 2L, 30L, false, 7, "作者", "words", "asc"))
                .thenReturn(Collections.emptyList());
        when(novelMapper.getTotalByCondition(USER_ID, false, 7, "作者"))
                .thenReturn(0L);

        NovelServiceImpl service = new NovelServiceImpl();
        service.setNovelMapper(novelMapper);

        assertEquals(0, service.getList(
                2, false, 7, " 作者 ", "words", "asc").getRecords().size());

        verify(novelMapper).getListByCondition(
                USER_ID, 2L, 30L, false, 7, "作者", "words", "asc");
        verify(novelMapper).getTotalByCondition(USER_ID, false, 7, "作者");
    }

    @Test
    void novelListRejectsUnsupportedSortBeforeQuerying() {
        NovelMapper novelMapper = mock(NovelMapper.class);
        NovelServiceImpl service = new NovelServiceImpl();
        service.setNovelMapper(novelMapper);

        assertThrows(IllegalArgumentException.class,
                () -> service.getList(1, false, null, null, "popular", "desc"));
        assertThrows(IllegalArgumentException.class,
                () -> service.getList(1, false, null, null, "created", "sideways"));

        verifyNoInteractions(novelMapper);
    }

    @Test
    void novelMapperOrdersBeforeApplyingPagination() throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        String resource = "mapper/NovelMapper.xml";
        try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(
                    inputStream, configuration, resource, configuration.getSqlFragments())
                    .parse();
        }

        assertSortSql(configuration, "created", "desc",
                "ORDER BY n.`create_time` DESC, n.`id` DESC LIMIT");
        assertSortSql(configuration, "words", "asc",
                "ORDER BY n.`total_words` ASC, n.`id` ASC LIMIT");
        assertSortSql(configuration, "updated", "desc",
                "ORDER BY n.`update_time` DESC, n.`id` DESC LIMIT");
    }

    private void assertSortSql(MybatisConfiguration configuration, String sortBy,
                               String sortOrder, String expectedSql) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", USER_ID);
        parameters.put("pageNum", 1L);
        parameters.put("pageSize", 30L);
        parameters.put("deleted", false);
        parameters.put("tagId", null);
        parameters.put("keyword", null);
        parameters.put("sortBy", sortBy);
        parameters.put("sortOrder", sortOrder);

        BoundSql boundSql = configuration
                .getMappedStatement("com.shiyq.mapper.NovelMapper.getListByCondition")
                .getBoundSql(parameters);
        String sql = boundSql.getSql().replaceAll("\\s+", " ");

        assertTrue(sql.contains(expectedSql), sql);
    }
}
