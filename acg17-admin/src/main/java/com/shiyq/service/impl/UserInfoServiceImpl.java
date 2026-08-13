package com.shiyq.service.impl;

import com.shiyq.entity.DO.UserInfo;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.UserInfoVO;
import com.shiyq.mapper.UserInfoMapper;
import com.shiyq.service.UserInfoService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Value("${serverFileUrlPrefix}")
    private String serverFileUrlPrefix;
    private UserInfoMapper userInfoMapper;

    @Autowired
    public void setUserInfoMapper(UserInfoMapper userInfoMapper) {
        this.userInfoMapper = userInfoMapper;
    }

    @Override
    public UserInfoVO getInfo() {
        UserInfoVO userInfo = userInfoMapper.selectUserInfoByUserId(UserContext.requireCurrentUserId());
        if (userInfo != null && userInfo.getAvatarUrl() != null && !userInfo.getAvatarUrl().trim().isEmpty()) {
            userInfo.setAvatarUrl(serverFileUrlPrefix + "avatar/" + userInfo.getAvatarUrl());
        }
        return userInfo;
    }
}
