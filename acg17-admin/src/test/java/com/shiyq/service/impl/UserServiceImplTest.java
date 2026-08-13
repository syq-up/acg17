package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiyq.entity.DO.User;
import com.shiyq.entity.VO.LoginVO;
import com.shiyq.mapper.UserMapper;
import com.shiyq.util.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {

    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private JWTUtil jwtUtil;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        jwtUtil = new JWTUtil("test-secret-with-at-least-32-bytes-long", 1);
        userService = new UserServiceImpl(userMapper, passwordEncoder, jwtUtil);
    }

    @Test
    void shouldAuthenticateWithBCryptAndReturnOnlyToken() {
        User user = user("correct-password");
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        LoginVO result = userService.login(" qiong ", "correct-password");

        assertNotNull(result);
        assertNotNull(result.getAccessToken());
        assertNotNull(user.getLastLoginTime());
        assertEquals(4, jwtUtil.verify(result.getAccessToken()).getClaim("authVersion").asInt());
        verify(userMapper).updateById(user);
    }

    @Test
    void shouldRejectWrongPasswordWithoutUpdatingUser() {
        User user = user("correct-password");
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        assertNull(userService.login("qiong", "wrong-password"));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void logoutShouldIncrementAuthenticationVersion() {
        User user = user("correct-password");
        when(userMapper.selectById(1)).thenReturn(user);

        userService.logout(1);

        assertEquals(5, user.getAuthVersion());
        verify(userMapper).updateById(user);
    }

    private User user(String rawPassword) {
        User user = new User();
        user.setId(1);
        user.setUsername("qiong");
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setAuthVersion(4);
        user.setDeleted(false);
        return user;
    }
}
