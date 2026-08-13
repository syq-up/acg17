package com.shiyq.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NovelTagRelationMapper {

    int insertRelation(@Param("novelId") int novelId,
                       @Param("tagId") int tagId,
                       @Param("userId") int userId);
}
