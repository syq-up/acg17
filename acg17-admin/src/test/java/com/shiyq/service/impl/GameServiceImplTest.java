package com.shiyq.service.impl;

import com.shiyq.entity.DTO.UserContext;
import com.shiyq.mapper.GameMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
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
}
