package com.shiyq.mapper;

import com.shiyq.entity.DO.User;
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
public interface UserMapper extends BaseMapper<User> {

    /**
     * 锁定用户行，串行化该用户的插画排序写操作。
     */
    Integer lockById(@Param("id") int id);
}
