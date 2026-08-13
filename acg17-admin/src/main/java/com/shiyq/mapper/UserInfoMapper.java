package com.shiyq.mapper;

import com.shiyq.entity.DO.UserInfo;
import com.shiyq.entity.VO.UserInfoVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    UserInfoVO selectUserInfoByUserId(@Param("userId") int userId);

}
