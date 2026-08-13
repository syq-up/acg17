package com.shiyq.controller;

import com.shiyq.entity.DO.Game;
import com.shiyq.entity.DTO.GameUpdateDTO;
import com.shiyq.entity.DTO.GameUploadDTO;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 游戏信息控制器
 *
 * @author shiyq
 * @since 2024-12-19
 */
@RestController
@RequestMapping("/game")
public class GameController {

    @Autowired
    private GameService gameService;

    @GetMapping("/list")
    public ResultVO getGameList(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "false") boolean deleted
    ) {
        PageVO<Game> result = gameService.getGameList(pageNum, title, deleted);
        return ResultVO.success("查询成功", result);
    }

    /**
     * 新增游戏（文件上传）
     *
     * @param gameUploadDTO 游戏上传DTO
     * @return 新增结果
     */
    @PostMapping("/addGame")
    public ResultVO addGame(@ModelAttribute GameUploadDTO gameUploadDTO) {
        try {
            String result = gameService.addGame(gameUploadDTO);
            return ResultVO.success("新增成功", result);
        } catch (Exception e) {
            return ResultVO.error("新增游戏失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID获取游戏详情
     *
     * @param id 游戏ID
     * @return 游戏详情
     */
    @GetMapping("/{id}")
    public ResultVO getGameById(@PathVariable Integer id) {
        try {
            Game game = gameService.getGameById(id);
            if (game != null) {
                return ResultVO.success("查询成功", game);
            } else {
                return ResultVO.error("游戏不存在");
            }
        } catch (Exception e) {
            return ResultVO.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新游戏信息
     *
     * @param id 游戏ID
     * @param gameUpdateDTO 可更新的游戏信息
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ResultVO updateGame(@PathVariable Integer id, @RequestBody GameUpdateDTO gameUpdateDTO) {
        try {
            boolean success = gameService.updateGame(id, gameUpdateDTO);
            if (success) {
                return ResultVO.success("更新成功");
            } else {
                return ResultVO.error("更新失败");
            }
        } catch (Exception e) {
            return ResultVO.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除游戏（逻辑删除）
     *
     * @param id 游戏ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResultVO deleteGame(@PathVariable Integer id) {
        try {
            boolean success = gameService.deleteGame(id);
            if (success) {
                return ResultVO.success("删除成功");
            } else {
                return ResultVO.error("删除失败");
            }
        } catch (Exception e) {
            return ResultVO.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 恢复已删除的游戏
     *
     * @param id 游戏ID
     * @return 操作结果
     */
    @PutMapping("/{id}/restore")
    public ResultVO restoreGame(@PathVariable Integer id) {
        try {
            boolean success = gameService.restoreGame(id);
            if (success) {
                return ResultVO.success("恢复游戏成功");
            } else {
                return ResultVO.error("恢复游戏失败");
            }
        } catch (Exception e) {
            return ResultVO.error("恢复游戏失败：" + e.getMessage());
        }
    }

    /**
     * 更新收藏状态
     *
     * @param id 游戏ID
     * @param favorite 收藏状态
     * @return 操作结果
     */
    @PutMapping("/{id}/favorite")
    public ResultVO updateFavorite(@PathVariable Integer id,
                                   @RequestParam boolean favorite) {
        try {
            boolean success = gameService.updateFavorite(id, favorite);
            if (success) {
                return ResultVO.success("更新收藏状态成功");
            } else {
                return ResultVO.error("更新收藏状态失败");
            }
        } catch (Exception e) {
            return ResultVO.error("更新收藏状态失败：" + e.getMessage());
        }
    }

    /**
     * 随机获取一个游戏
     * @return 随机游戏
     */
    @GetMapping("/random")
    public ResultVO getRandomGame() {
        try {
            Game game = gameService.getRandomGame();
            if (game != null) {
                return ResultVO.success("查询成功", game);
            } else {
                return ResultVO.error("没有可用游戏");
            }
        } catch (Exception e) {
            return ResultVO.error("查询失败：" + e.getMessage());
        }
    }

}
