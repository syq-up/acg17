package com.shiyq.service;

import com.shiyq.entity.DO.Illustration;
import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.VO.IllustrationVO;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.entity.VO.ReorderRequest;
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
public interface IllustrationService extends IService<Illustration> {

    /**
     * 上传（单张插画作品）
     */
    IllustrationVO upload(MultipartFile file) throws IOException;

    /**
     * 插入插画记录
     */
    IllustrationVO insertIllustration(Illustration illustration);

    /**
     * 分页获取插画作品
     */
    PageVO<IllustrationVO> getList(long pageNum, boolean deleted);

    /**
     * 随机获取一张插画作品
     */
    IllustrationVO getRandomIllustration();

    /**
     * 逻辑删除，并自动填充更新字段
     */
    boolean deleteById(int id);

    /**
     * 回收已逻辑删除的插画作品
     */
    boolean restoreById(int id);

    /**
     * 更改插画的排序位置
     */
    boolean reorder(ReorderRequest reorderRequest);
}
