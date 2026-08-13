package com.shiyq.service;

import com.shiyq.entity.DO.UserInfo;
import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.VO.UserInfoVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
public interface UserInfoService extends IService<UserInfo> {

    /**
     * 获取用户信息
     */
    UserInfoVO getInfo();

}
