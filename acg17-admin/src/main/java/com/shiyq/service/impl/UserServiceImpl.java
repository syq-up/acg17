package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shiyq.entity.DO.User;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.LoginVO;
import com.shiyq.mapper.UserMapper;
import com.shiyq.service.UserService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.shiyq.util.JWTUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        User user = userMapper.selectById(UserContext.requireCurrentUserId());
        if (user == null) {
            throw new IllegalStateException("当前用户不存在");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        int authVersion = user.getAuthVersion() == null ? 1 : user.getAuthVersion();
        user.setAuthVersion(authVersion + 1);
        if (userMapper.updateById(user) != 1) {
            throw new IllegalStateException("修改密码失败");
        }
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
