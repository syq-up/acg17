package com.shiyq.controller;

import com.shiyq.entity.DO.Game;
import com.shiyq.entity.DTO.GameUpdateDTO;
import com.shiyq.entity.DTO.GameUploadDTO;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.exception.ApiException;
import com.shiyq.service.GameService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestParam(defaultValue = "1") @Positive(message = "页码必须大于0") long pageNum,
            @RequestParam(required = false) @Size(max = 255, message = "游戏标题不能超过255个字符") String title,
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
    public ResponseEntity<ResultVO> addGame(@Valid @ModelAttribute GameUploadDTO gameUploadDTO) throws Exception {
        String result = gameService.addGame(gameUploadDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultVO.created("新增成功", result));
    }

    /**
     * 根据ID获取游戏详情
     *
     * @param id 游戏ID
     * @return 游戏详情
     */
    @GetMapping("/{id}")
    public ResultVO getGameById(@PathVariable @Positive(message = "游戏ID必须大于0") Integer id) {
        Game game = gameService.getGameById(id);
        if (game == null) {
            throw ApiException.notFound("游戏不存在");
        }
        return ResultVO.success("查询成功", game);
    }

    /**
     * 更新游戏信息
     *
     * @param id 游戏ID
     * @param gameUpdateDTO 可更新的游戏信息
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ResultVO updateGame(@PathVariable @Positive(message = "游戏ID必须大于0") Integer id,
                               @Valid @RequestBody GameUpdateDTO gameUpdateDTO) {
        if (!gameService.updateGame(id, gameUpdateDTO)) {
            throw ApiException.notFound("游戏不存在或已删除");
        }
        return ResultVO.success("更新成功");
    }

    /**
     * 删除游戏（逻辑删除）
     *
     * @param id 游戏ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResultVO deleteGame(@PathVariable @Positive(message = "游戏ID必须大于0") Integer id) {
        if (!gameService.deleteGame(id)) {
            throw ApiException.notFound("游戏不存在或已删除");
        }
        return ResultVO.success("删除成功");
    }

    /**
     * 恢复已删除的游戏
     *
     * @param id 游戏ID
     * @return 操作结果
     */
    @PutMapping("/{id}/restore")
    public ResultVO restoreGame(@PathVariable @Positive(message = "游戏ID必须大于0") Integer id) {
        if (!gameService.restoreGame(id)) {
            throw ApiException.notFound("游戏不存在或不在回收站中");
        }
        return ResultVO.success("恢复游戏成功");
    }

    /**
     * 更新收藏状态
     *
     * @param id 游戏ID
     * @param favorite 收藏状态
     * @return 操作结果
     */
    @PutMapping("/{id}/favorite")
    public ResultVO updateFavorite(@PathVariable @Positive(message = "游戏ID必须大于0") Integer id,
                                   @RequestParam boolean favorite) {
        if (!gameService.updateFavorite(id, favorite)) {
            throw ApiException.notFound("游戏不存在或已删除");
        }
        return ResultVO.success("更新收藏状态成功");
    }

    /**
     * 随机获取一个游戏
     * @return 随机游戏
     */
    @GetMapping("/random")
    public ResultVO getRandomGame() {
        Game game = gameService.getRandomGame();
        if (game == null) {
            throw ApiException.notFound("没有可用游戏");
        }
        return ResultVO.success("查询成功", game);
    }

}
