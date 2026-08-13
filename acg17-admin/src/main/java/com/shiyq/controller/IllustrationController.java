package com.shiyq.controller;

import com.shiyq.entity.VO.ReorderRequest;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.service.IllustrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResultVO upload(@RequestParam("file") MultipartFile file) throws Exception {
        return ResultVO.success(illustrationService.upload(file));
    }

    /**
     * 分页获取插画作品
     */
    @GetMapping("/getList")
    public ResultVO getList(@RequestParam long pageNum) {
        return ResultVO.success(illustrationService.getList(pageNum, false));
    }

    /**
     * 分页获取已逻辑删除的插画作品
     */
    @GetMapping("/getRecycleList")
    public ResultVO getRecycleList(@RequestParam long pageNum) {
        return ResultVO.success(illustrationService.getList(pageNum, true));
    }

    /**
     * 随机获取一张插画作品
     */
    @GetMapping("/getRandomArtwork")
    public ResultVO getRandomArtwork() {
        return ResultVO.success(illustrationService.getRandomIllustration());
    }

    /**
     * 逻辑删除一张插画
     */
    @GetMapping("/deleteById/{id}")
    public ResultVO deleteById(@PathVariable int id) {
        return illustrationService.deleteById(id)
                ? ResultVO.success("OK")
                : ResultVO.error("失败，请稍后尝试……");
    }

    /**
     * 回收一张逻辑删除的插画
     */
    @GetMapping("/restoreById/{id}")
    public ResultVO restoreById(@PathVariable int id) {
        return illustrationService.restoreById(id)
                ? ResultVO.success("OK")
                : ResultVO.error("失败，请稍后尝试……");
    }

    /**
     * 重新排序插画位置
     */
    @PostMapping("/reorder")
    public ResultVO reorder(@RequestBody ReorderRequest reorderRequest) {
        return illustrationService.reorder(reorderRequest)
                ? ResultVO.success("OK")
                : ResultVO.error("自定义排序失败，请稍后尝试……");
    }

}
