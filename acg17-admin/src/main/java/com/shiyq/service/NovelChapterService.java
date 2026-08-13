package com.shiyq.service;

import com.shiyq.entity.DO.NovelChapter;
import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.DTO.NovelChapterCreateDTO;
import com.shiyq.entity.DTO.NovelChapterUpdateDTO;
import com.shiyq.entity.VO.NovelChapterDetailVO;
import com.shiyq.entity.VO.NovelChapterVO;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
public interface NovelChapterService extends IService<NovelChapter> {

    /**
     * 获取指定小说章节列表
     */
    List<NovelChapterVO> getList(int novelId);

    NovelChapterDetailVO getContentById(int id);

    boolean addChapter(NovelChapterCreateDTO request);

    boolean updateChapter(NovelChapterUpdateDTO request);

}
