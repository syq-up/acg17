package com.shiyq.controller;

import com.shiyq.entity.DTO.NovelChapterCreateDTO;
import com.shiyq.entity.DTO.NovelChapterUpdateDTO;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.exception.ApiException;
import com.shiyq.service.NovelChapterService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResultVO getList(@PathVariable @Positive(message = "小说ID必须大于0") int novelId) {
        return ResultVO.success(chapterService.getList(novelId));
    }

    @GetMapping("/getContentById/{id}")
    public ResultVO getContentById(@PathVariable @Positive(message = "章节ID必须大于0") int id) {
        var chapter = chapterService.getContentById(id);
        if (chapter == null) {
            throw ApiException.notFound("小说章节不存在");
        }
        return ResultVO.success(chapter);
    }

    @PostMapping("/addChapter")
    public ResponseEntity<ResultVO> addChapter(@Valid @RequestBody NovelChapterCreateDTO request) {
        if (!chapterService.addChapter(request)) {
            throw ApiException.notFound("小说不存在或已删除");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultVO.created("新增章节成功", null));
    }

    @PostMapping("/updateChapter")
    public ResultVO updateChapter(@Valid @RequestBody NovelChapterUpdateDTO request) {
        if (!chapterService.updateChapter(request)) {
            throw ApiException.notFound("小说章节不存在");
        }
        return ResultVO.success("更新章节成功");
    }

}
