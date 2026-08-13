package com.shiyq.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.shiyq.entity.DO.Game;
import com.shiyq.entity.DTO.GameUpdateDTO;
import com.shiyq.entity.DTO.GameUploadDTO;
import com.shiyq.entity.VO.PageVO;

/**
 * 游戏信息服务接口
 *
 * @author shiyq
 * @since 2024-12-19
 */
public interface GameService extends IService<Game> {

    /**
     * 分页获取游戏列表（返回PageVO）
     * @param pageNum 页码
     * @param title 游戏名称（模糊查询，同时查询title和chineseTitle）
     * @param deleted 是否查询已删除的记录
     * @return 分页结果
     */
    PageVO<Game> getGameList(long pageNum, String title, boolean deleted);

    /**
     * 新增游戏
     *
     * @param gameUploadDTO 游戏上传DTO
     * @return 游戏标题
     */
    String addGame(GameUploadDTO gameUploadDTO) throws Exception;

    /**
     * 根据ID获取游戏详情
     *
     * @param id 游戏ID
     * @return 游戏信息
     */
    Game getGameById(Integer id);

    /**
     * 更新游戏信息
     *
     * @param id 游戏ID
     * @param gameUpdateDTO 可更新的游戏信息
     * @return 是否成功
     */
    boolean updateGame(Integer id, GameUpdateDTO gameUpdateDTO);

    /**
     * 根据ID删除游戏（逻辑删除）
     *
     * @param id 游戏ID
     * @return 是否成功
     */
    boolean deleteGame(Integer id);

    /**
     * 恢复已删除的游戏
     *
     * @param id 游戏ID
     * @return 是否成功
     */
    boolean restoreGame(Integer id);

    /**
     * 更新收藏状态
     *
     * @param id 游戏ID
     * @param favorite 收藏状态
     * @return 是否成功
     */
    boolean updateFavorite(Integer id, boolean favorite);

    /**
     * 随机获取一个游戏
     * @return 随机游戏
     */
    Game getRandomGame();


}
