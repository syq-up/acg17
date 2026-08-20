package com.shiyq.controller;

import com.shiyq.entity.VO.ReorderRequest;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.entity.VO.IllustrationVO;
import com.shiyq.exception.ApiException;
import com.shiyq.service.IllustrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@RestController
@RequestMapping("/illustration")
public class IllustrationController {

    private IllustrationService illustrationService;

    @Autowired
    public void setIllustrationService(IllustrationService illustrationService) {
        this.illustrationService = illustrationService;
    }

    /**
     * 【单张插画作品】上传
     */
    @PostMapping("/upload")
    public ResponseEntity<ResultVO> upload(@RequestParam("file") MultipartFile file) throws IOException {
        IllustrationVO illustration = illustrationService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultVO.created("上传成功", illustration));
    }

    /**
     * 分页获取插画作品
     */
    @GetMapping("/getList")
    public ResultVO getList(@RequestParam @Positive(message = "页码必须大于0") long pageNum) {
        return ResultVO.success(illustrationService.getList(pageNum, false));
    }

    /**
     * 分页获取已逻辑删除的插画作品
     */
    @GetMapping("/getRecycleList")
    public ResultVO getRecycleList(@RequestParam @Positive(message = "页码必须大于0") long pageNum) {
        return ResultVO.success(illustrationService.getList(pageNum, true));
    }

    /**
     * 随机获取一张插画作品
     */
    @GetMapping("/random")
    public ResultVO getRandomIllustration() {
        IllustrationVO illustration = illustrationService.getRandomIllustration();
        if (illustration == null) {
            throw ApiException.notFound("没有可用插画");
        }
        return ResultVO.success(illustration);
    }

    /**
     * 逻辑删除一张插画
     */
    @DeleteMapping("/{id}")
    public ResultVO deleteById(@PathVariable @Positive(message = "插画ID必须大于0") int id) {
        if (!illustrationService.deleteById(id)) {
            throw ApiException.notFound("插画不存在或已删除");
        }
        return ResultVO.success("OK");
    }

    /**
     * 回收一张逻辑删除的插画
     */
    @PutMapping("/{id}/restore")
    public ResultVO restoreById(@PathVariable @Positive(message = "插画ID必须大于0") int id) {
        if (!illustrationService.restoreById(id)) {
            throw ApiException.notFound("插画不存在或不在回收站中");
        }
        return ResultVO.success("OK");
    }

    /**
     * 重新排序插画位置
     */
    @PostMapping("/reorder")
    public ResultVO reorder(@Valid @RequestBody ReorderRequest reorderRequest) {
        if (!illustrationService.reorder(reorderRequest)) {
            throw ApiException.notFound("待排序的插画不存在");
        }
        return ResultVO.success("OK");
    }

}
