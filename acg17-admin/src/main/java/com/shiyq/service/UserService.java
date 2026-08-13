package com.shiyq.service;

import com.shiyq.entity.DO.User;
import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.VO.LoginVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
public interface UserService extends IService<User> {

    /**
     * 用户登录
     */
    LoginVO login(String username, String password);

    /**
     * 使该用户已签发的令牌全部失效
     */
    void logout(Integer userId);

}
