package com.shiyq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shiyq.entity.DO.Game;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 游戏信息 Mapper 接口
 *
 * @author shiyq
 * @since 2024-12-19
 */
@Mapper
public interface GameMapper extends BaseMapper<Game> {

    /**
     * 根据ID和用户ID查询游戏
     *
     * @param id 游戏ID
     * @param userId 用户ID
     * @return 游戏信息
     */
    Game selectByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 逻辑删除游戏
     *
     * @param id 游戏ID
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 恢复已删除的游戏
     *
     * @param id 游戏ID
     * @param userId 用户ID
     * @return 影响行数
     */
    int restoreByIdAndUserId(@Param("id") Integer id, @Param("userId") Integer userId);

    /**
     * 更新收藏状态
     *
     * @param id 游戏ID
     * @param favorite 收藏状态
     * @param userId 用户ID
     * @return 影响行数
     */
    int updateFavoriteByIdAndUserId(@Param("id") Integer id,
                                    @Param("favorite") boolean favorite,
                                    @Param("userId") Integer userId);

    /**
     * 查询达到回收站保留期限的游戏ID
     */
    List<Integer> getExpiredIds(@Param("cutoff") Date cutoff);

    /**
     * 锁定并读取仍满足清理条件的游戏
     */
    Game selectExpiredByIdForUpdate(@Param("id") Integer id, @Param("cutoff") Date cutoff);

    /**
     * 物理删除仍满足清理条件的游戏
     */
    int realDeleteExpiredById(@Param("id") Integer id, @Param("cutoff") Date cutoff);

    /**
     * 根据条件查询游戏列表
     * @param userId 用户ID
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param deleted 是否已删除
     * @param title 标题（模糊查询）
     * @return 游戏列表
     */
    List<Game> getListByCondition(@Param("userId") Integer userId, 
                                  @Param("pageNum") Long pageNum, 
                                  @Param("pageSize") Long pageSize, 
                                  @Param("deleted") boolean deleted,
                                  @Param("title") String title);

    /**
     * 根据条件查询总记录数
     * @param userId 用户ID
     * @param deleted 是否已删除
     * @param title 标题（模糊查询）
     * @return 总记录数
     */
    Long getTotalByCondition(@Param("userId") Integer userId, 
                            @Param("deleted") boolean deleted,
                            @Param("title") String title);

    /**
     * 随机获取一个游戏
     * @param userId 用户ID
     * @return 随机游戏
     */
    Game getRandomRecord(@Param("userId") Integer userId);

    /**
     * 查询所有仍有数据库记录的游戏ID，用于保护逻辑删除记录对应的文件目录
     */
    List<Integer> getAllIdsForFileCleanup();

}
