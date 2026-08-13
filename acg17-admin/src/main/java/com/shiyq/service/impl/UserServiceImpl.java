package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiyq.entity.DO.User;
import com.shiyq.entity.VO.LoginVO;
import com.shiyq.mapper.UserMapper;
import com.shiyq.service.UserService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.shiyq.util.JWTUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JWTUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public LoginVO login(String username, String password) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username.trim());
        User user = userMapper.selectOne(queryWrapper);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        user.setLastLoginTime(new Date());
        userMapper.updateById(user);
        return new LoginVO(jwtUtil.getLoginToken(user.getId(), user.getAuthVersion()));
    }

    @Override
    public void logout(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        user.setAuthVersion(user.getAuthVersion() + 1);
        userMapper.updateById(user);
    }
}
