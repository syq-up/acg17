package com.shiyq.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.DO.NovelTag;

import java.util.List;

public interface NovelTagService extends IService<NovelTag> {

    List<NovelTag> listAllTags();

    List<String> assignTags(int novelId, List<String> tagNames);
}
