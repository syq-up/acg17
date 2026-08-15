package com.shiyq.service;

import com.shiyq.entity.DO.UserInfo;
import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.VO.UserInfoVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

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

    /**
     * 修改当前用户昵称。
     */
    UserInfoVO updateNickname(String nickname);

    /**
     * 上传并替换当前用户头像。
     */
    UserInfoVO updateAvatar(MultipartFile file) throws IOException;

    /**
     * 移除当前用户头像，恢复默认头像。
     */
    UserInfoVO resetAvatar() throws IOException;

}
