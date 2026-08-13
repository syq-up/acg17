package com.shiyq.service;

import com.shiyq.entity.DO.Novel;
import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.DTO.NovelCreateDTO;
import com.shiyq.entity.VO.NovelVO;
import com.shiyq.entity.VO.PageVO;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
public interface NovelService extends IService<Novel> {

    /**
     * 分页获取小说作品
     */
    PageVO<NovelVO> getList(long pageNum, boolean deleted, Integer tagId, String keyword);

    /**
     * 获取小说作品信息
     */
    NovelVO getNovelById(int id);

    /**
     * 新增小说
     */
    NovelVO addNovel(NovelCreateDTO request);

    /**
     * 删除小说
     */
    boolean deleteNovelById(int id);

    /**
     * 恢复小说
     */
    boolean restoreNovelById(int id);

}
