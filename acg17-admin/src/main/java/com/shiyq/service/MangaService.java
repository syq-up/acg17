package com.shiyq.service;

import com.shiyq.entity.DO.Manga;
import com.shiyq.entity.DTO.MangaChapterUploadDTO;
import com.shiyq.entity.DTO.MangaUpdateDTO;
import com.shiyq.entity.DTO.MangaUploadDTO;
import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.VO.MangaChapterVO;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.entity.VO.MangaVO;
import com.shiyq.entity.VO.MangaDetailVO;

import java.util.List;

/**
 * <p>
 * 漫画 服务类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
public interface MangaService extends IService<Manga> {

    /**
     * 分页获取漫画列表
     * @param pageNum 页码
     * @param deleted 是否查询已删除的记录
     * @param title 标题（模糊查询，同时查询title和chinese_title）
     * @param tagIds 标签ID列表，漫画需要同时包含全部标签
     * @return 分页结果
     */
    PageVO<MangaVO> getList(long pageNum, boolean deleted, String title, List<Integer> tagIds);

    /**
     * 根据ID获取漫画详情
     * @param id 漫画ID
     * @return 漫画详情
     */
    MangaDetailVO getMangaById(long id);

    /**
     * 新增漫画（使用DTO）
     * @param mangaUploadDTO 漫画上传DTO
     * @return 漫画标题
     */
    String addManga(MangaUploadDTO mangaUploadDTO) throws Exception;

    /**
     * 为当前用户的漫画追加一个章节。
     * @param mangaId 漫画ID
     * @param mangaChapterUploadDTO 章节标题和ZIP文件
     * @return 新章节信息
     */
    MangaChapterVO addMangaChapter(long mangaId,
                                   MangaChapterUploadDTO mangaChapterUploadDTO);

    /**
     * 更新漫画基础信息
     */
    boolean updateManga(long id, MangaUpdateDTO mangaUpdateDTO);

    /**
     * 逻辑删除漫画
     * @param id 漫画ID
     * @return 是否删除成功
     */
    boolean deleteMangaById(long id);

    /**
     * 恢复漫画
     * @param id 漫画ID
     * @return 是否恢复成功
     */
    boolean restoreMangaById(long id);

    /**
     * 更新漫画收藏状态
     * @param id 漫画ID
     * @param favorite 收藏状态
     * @return 是否更新成功
     */
    boolean updateFavoriteStatus(long id, boolean favorite);

    /**
     * 为漫画添加标签
     * @param mangaId 漫画ID
     * @param tagId 标签ID
     * @return 是否添加成功
     */
    boolean addTagToManga(long mangaId, Integer tagId);

    /**
     * 从漫画中删除标签
     * @param mangaId 漫画ID
     * @param tagId 标签ID
     * @return 是否删除成功
     */
    boolean removeTagFromManga(long mangaId, Integer tagId);

    /**
     * 物理删除漫画页面（包含漫画图片）
     * @param mangaId 漫画ID
     * @param chapterId 章节ID
     * @param pageNum 页面编号
     * @return 是否删除成功
     */
    boolean realDeleteMangaPage(int mangaId, int chapterId, int pageNum);

    /**
     * 随机获取一个漫画
     * @return 随机漫画
     */
    MangaDetailVO getRandomManga();

}
