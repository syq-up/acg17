package com.shiyq.service.impl;

import com.shiyq.entity.DO.Illustration;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.IllustrationVO;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.entity.VO.ReorderRequest;
import com.shiyq.mapper.IllustrationMapper;
import com.shiyq.mapper.UserMapper;
import com.shiyq.service.MediaUrlSigner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IllustrationServiceImplTest {

    private static final int USER_ID = 7;

    private IllustrationMapper illustrationMapper;
    private UserMapper userMapper;
    private IllustrationServiceImpl service;

    @BeforeEach
    void setUp() {
        illustrationMapper = mock(IllustrationMapper.class);
        userMapper = mock(UserMapper.class);
        when(userMapper.lockById(USER_ID)).thenReturn(USER_ID);
        service = new IllustrationServiceImpl();
        service.setIllustrationMapper(illustrationMapper);
        service.setUserMapper(userMapper);
        MediaUrlSigner mediaUrlSigner = mock(MediaUrlSigner.class);
        when(mediaUrlSigner.sign(anyString(), anyString()))
                .thenReturn("/api/media?path=signed");
        service.setMediaUrlSigner(mediaUrlSigner);
        ReflectionTestUtils.setField(service, "illustrationFolder", "illustrations/upload");
        UserContext.add(USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void reorderTowardFrontDecrementsTheWholeRange() {
        Illustration source = illustration(11, 3);
        Illustration target = illustration(22, 8);
        when(illustrationMapper.selectActiveByIdForUpdate(11, USER_ID)).thenReturn(source);
        when(illustrationMapper.selectActiveByIdForUpdate(22, USER_ID)).thenReturn(target);
        when(illustrationMapper.moveSortOrderToTemporary(11, USER_ID)).thenReturn(1);
        when(illustrationMapper.updateSortOrderByIdAndUserId(11, 8, USER_ID)).thenReturn(1);

        assertTrue(service.reorder(request(11, 22)));

        verify(userMapper).lockById(USER_ID);
        verify(illustrationMapper).moveSortOrderToTemporary(11, USER_ID);
        verify(illustrationMapper).decrementSortOrderRange(USER_ID, 3, 8);
        verify(illustrationMapper).updateSortOrderByIdAndUserId(11, 8, USER_ID);
    }

    @Test
    void reorderTowardBackIncrementsTheWholeRange() {
        Illustration source = illustration(11, 8);
        Illustration target = illustration(22, 3);
        when(illustrationMapper.selectActiveByIdForUpdate(11, USER_ID)).thenReturn(source);
        when(illustrationMapper.selectActiveByIdForUpdate(22, USER_ID)).thenReturn(target);
        when(illustrationMapper.moveSortOrderToTemporary(11, USER_ID)).thenReturn(1);
        when(illustrationMapper.updateSortOrderByIdAndUserId(11, 3, USER_ID)).thenReturn(1);

        assertTrue(service.reorder(request(11, 22)));

        verify(userMapper).lockById(USER_ID);
        verify(illustrationMapper).moveSortOrderToTemporary(11, USER_ID);
        verify(illustrationMapper).incrementSortOrderRange(USER_ID, 3, 8);
        verify(illustrationMapper).updateSortOrderByIdAndUserId(11, 3, USER_ID);
    }

    @Test
    void randomIllustrationReturnsNullWhenNoRecordExists() {
        when(illustrationMapper.getRandomRecord()).thenReturn(null);

        assertNull(service.getRandomIllustration());
    }

    @Test
    void listReturnsOriginalAndSmallStyleUrlsFromTheOriginalFile() {
        Illustration illustration = illustration(11, 3);
        illustration.setPath("image.png");
        when(illustrationMapper.getListByCondition(USER_ID, 1L, 36L, false))
                .thenReturn(List.of(illustration));
        when(illustrationMapper.getTotalByCondition(USER_ID, false)).thenReturn(1L);

        PageVO<IllustrationVO> page = service.getList(1L, false);

        IllustrationVO result = page.getRecords().getFirst();
        assertEquals("/api/media?path=signed", result.getOriginalUrl());
        assertEquals("/api/media?path=signed&style=small", result.getThumbnailUrl());
    }

    private Illustration illustration(int id, int sortOrder) {
        Illustration illustration = new Illustration(id);
        illustration.setSortOrder(sortOrder);
        return illustration;
    }

    private ReorderRequest request(int id, int targetId) {
        ReorderRequest request = new ReorderRequest();
        request.setId(id);
        request.setTargetId(targetId);
        return request;
    }
}
