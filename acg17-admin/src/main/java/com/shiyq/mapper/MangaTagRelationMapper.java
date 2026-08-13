package com.shiyq.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MangaTagRelationMapper {

    int insertRelation(@Param("mangaId") Integer mangaId,
                       @Param("tagId") Integer tagId,
                       @Param("userId") Integer userId);

    int deleteRelation(@Param("mangaId") Integer mangaId,
                       @Param("tagId") Integer tagId,
                       @Param("userId") Integer userId);

    long countByTagId(@Param("tagId") Integer tagId, @Param("userId") Integer userId);
}
