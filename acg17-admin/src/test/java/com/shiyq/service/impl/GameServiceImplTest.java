package com.shiyq.service.impl;

import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.DTO.GameUploadDTO;
import com.shiyq.mapper.GameMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GameServiceImplTest {

    private static final int USER_ID = 19;

    private GameMapper gameMapper;
    private GameServiceImpl service;

    @BeforeEach
    void setUp() {
        gameMapper = mock(GameMapper.class);
        service = new GameServiceImpl();
        service.setGameMapper(gameMapper);
        UserContext.add(USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.remove();
    }

    @Test
    void randomGameReturnsNullWhenNoRecordExists() {
        when(gameMapper.getRandomRecord(USER_ID)).thenReturn(null);

        assertNull(service.getRandomGame());
    }

    @Test
    void gameUploadRejectsOversizedImagesBeforeStorage() {
        ReflectionTestUtils.setField(service, "maxGameImageFileSize", "1B");
        GameUploadDTO request = new GameUploadDTO();
        request.setTitle("game");
        request.setCover(new MockMultipartFile(
                "cover", "cover.png", "image/png", new byte[] {1, 2}));

        assertThrows(IllegalArgumentException.class, () -> service.addGame(request));
    }

    @Test
    void gameUploadRejectsMoreThanTwentyPreviewImages() {
        GameUploadDTO request = new GameUploadDTO();
        request.setTitle("game");
        request.setCover(new MockMultipartFile(
                "cover", "cover.png", "image/png", new byte[] {1}));
        request.setPreviewImages(new MockMultipartFile[21]);

        assertThrows(IllegalArgumentException.class, () -> service.addGame(request));
    }
}
