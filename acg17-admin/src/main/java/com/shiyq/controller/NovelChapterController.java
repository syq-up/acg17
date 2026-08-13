package com.shiyq.controller;

import com.shiyq.entity.DTO.NovelChapterCreateDTO;
import com.shiyq.entity.DTO.NovelChapterUpdateDTO;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.service.NovelChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@RestController
@RequestMapping("/novel-chapter")
public class NovelChapterController {

    private NovelChapterService chapterService;

    @Autowired
    public void setChapterService(NovelChapterService chapterService) {
        this.chapterService = chapterService;
    }

    /**
     * 获取小说章节列表
     */
    @GetMapping("/getList/{novelId}")
    public ResultVO getList(@PathVariable int novelId) {
        return ResultVO.success(chapterService.getList(novelId));
    }

    @GetMapping("/getContentById/{id}")
    public ResultVO getContentById(@PathVariable int id) {
        return ResultVO.success(chapterService.getContentById(id));
    }

    @PostMapping("/addChapter")
    public ResultVO addChapter(@RequestBody NovelChapterCreateDTO request) {
        return chapterService.addChapter(request) ? ResultVO.success() : ResultVO.error("新增章节失败...");
    }

    @PostMapping("/updateChapter")
    public ResultVO updateChapter(@RequestBody NovelChapterUpdateDTO request) {
        return chapterService.updateChapter(request) ? ResultVO.success() : ResultVO.error("更新章节失败...");
    }

}
