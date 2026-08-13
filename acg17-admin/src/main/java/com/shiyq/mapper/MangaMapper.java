package com.shiyq.mapper;

import com.shiyq.entity.DO.Manga;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Date;

/**
 * <p>
 * 漫画 Mapper 接口
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Mapper
public interface MangaMapper extends BaseMapper<Manga> {

    /**
     * 根据条件查询漫画列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param deleted 是否已删除
     * @param author 作者（模糊查询）
     * @param title 标题（模糊查询）
     * @param tagId 标签ID
     * @return 漫画列表
     */
    List<Manga> getListByCondition(@Param("userId") Integer userId, 
                                   @Param("pageNum") Long pageNum, 
                                   @Param("pageSize") Long pageSize, 
                                   @Param("deleted") Boolean deleted,
                                   @Param("author") String author,
                                   @Param("title") String title,
                                   @Param("tagId") Integer tagId);

    /**
     * 根据条件查询总记录数
     * @param userId 用户ID
     * @param deleted 是否已删除
     * @param author 作者（模糊查询）
     * @param title 标题（模糊查询）
     * @param tagId 标签ID
     * @return 总记录数
     */
    Long getTotalByCondition(@Param("userId") Integer userId, 
                            @Param("deleted") Boolean deleted,
                            @Param("author") String author,
                            @Param("title") String title,
                            @Param("tagId") Integer tagId);

    /**
     * 根据ID查询漫画详情
     * @param id 漫画ID
     * @param userId 当前用户ID
     * @return 漫画详情
     */
    Manga getMangaDetailById(@Param("id") Long id, @Param("userId") Integer userId);

    /**
     * 查询当前用户未删除的漫画
     */
    Manga getOwnedMangaById(@Param("id") Long id, @Param("userId") Integer userId);

    /**
     * 锁定并读取当前用户未删除的漫画，用于串行修改章节数据。
     */
    Manga selectOwnedMangaByIdForUpdate(@Param("id") Long id,
                                        @Param("userId") Integer userId);

    /**
     * 更新漫画章节数据和文件总大小。
     */
    int updateChapterData(@Param("id") Long id,
                          @Param("userId") Integer userId,
                          @Param("pages") String pages,
                          @Param("size") Long size);

    /**
     * 逻辑删除当前用户的漫画
     */
    int deleteMangaByIdAndUserId(@Param("id") Long id, @Param("userId") Integer userId);

    /**
     * 恢复漫画
     * @param id 漫画ID
     * @param userId 当前用户ID
     * @return 受影响的行数
     */
    int restoreMangaById(@Param("id") Long id, @Param("userId") Integer userId);

    /**
     * 更新当前用户漫画的收藏状态
     */
    int updateFavoriteByIdAndUserId(@Param("id") Long id,
                                    @Param("favorite") Boolean favorite,
                                    @Param("userId") Integer userId);

    /**
     * 查询达到回收站保留期限的漫画ID
     */
    List<Integer> getExpiredIds(@Param("cutoff") Date cutoff);

    /**
     * 锁定并读取仍满足清理条件的漫画
     */
    Manga selectExpiredByIdForUpdate(@Param("id") Integer id, @Param("cutoff") Date cutoff);

    /**
     * 物理删除仍满足清理条件的漫画
     */
    int realDeleteExpiredById(@Param("id") Integer id, @Param("cutoff") Date cutoff);

    /**
     * 查询所有仍有数据库记录的漫画，用于保护逻辑删除记录对应的文件目录
     */
    List<Manga> getAllForFileCleanup();

    /**
     * 随机获取一个漫画
     * @param userId 用户ID
     * @return 随机漫画
     */
    Manga getRandomRecord(@Param("userId") Integer userId);

}
