package com.shiyq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyq.entity.DO.NovelTag;
import com.shiyq.entity.DTO.NovelTagAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NovelTagMapper extends BaseMapper<NovelTag> {

    NovelTag selectByName(@Param("name") String name, @Param("userId") Integer userId);

    List<NovelTagAssignment> listAssignmentsByNovelIds(@Param("novelIds") List<Integer> novelIds,
                                                       @Param("userId") Integer userId);
}
