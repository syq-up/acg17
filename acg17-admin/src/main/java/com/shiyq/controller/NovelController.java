package com.shiyq.controller;

import com.shiyq.entity.DTO.NovelCreateDTO;
import com.shiyq.entity.VO.*;
import com.shiyq.exception.ApiException;
import com.shiyq.service.NovelChapterService;
import com.shiyq.service.NovelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@RestController
@RequestMapping("/novel")
public class NovelController {

    private NovelService novelService;
    private NovelChapterService chapterService;

    @Autowired
    public void setNovelService(NovelService novelService) {
        this.novelService = novelService;
    }

    @Autowired
    public void setChapterService(NovelChapterService chapterService) {
        this.chapterService = chapterService;
    }

    /**
     * 分页获取小说作品
     */
    @GetMapping("/getList")
    public ResultVO getList(@RequestParam long pageNum,
                            @RequestParam(defaultValue = "false") boolean deleted,
                            @RequestParam(required = false) Integer tagId,
                            @RequestParam(required = false) String keyword) {
        return ResultVO.success(novelService.getList(pageNum, deleted, tagId, keyword));
    }

    /**
     * 获取小说作品
     */
    @GetMapping("/getNovelById/{id}")
    public ResultVO getNovelById(@PathVariable int id) {
        NovelVO vo = novelService.getNovelById(id);
        if (vo == null) {
            throw ApiException.notFound("小说作品不存在");
        }
        return ResultVO.success(vo);
    }

    /**
     * 获取小说内容（章节列表，第一章内容）
     */
    @GetMapping("/getContentById/{id}")
    public ResultVO getContentById(@PathVariable int id) {
        // 查小说
        NovelVO novelVO = novelService.getNovelById(id);
        if (novelVO == null) {
            throw ApiException.notFound("小说作品不存在");
        }
        ResultVO result = ResultVO.success(novelVO);
        // 查章节
        List<NovelChapterVO> chapterVOList = chapterService.getList(id);
        result.put("chapterList", chapterVOList);
        if (chapterVOList.size() == 0) {
            return result;
        }
        // 查第一章内容
        NovelChapterDetailVO contentVO = chapterService.getContentById(chapterVOList.get(0).getId());
        result.put("firstChapter", contentVO);

        return result;
    }

    @PostMapping("/addNovel")
    public ResponseEntity<ResultVO> addNovel(@RequestBody NovelCreateDTO request) {
        NovelVO novel = novelService.addNovel(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultVO.created("新增小说成功", novel));
    }

    /**
     * 删除小说
     */
    @GetMapping("/deleteById/{id}")
    public ResultVO deleteById(@PathVariable int id) {
        if (!novelService.deleteNovelById(id)) {
            throw ApiException.notFound("小说作品不存在或已删除");
        }
        return ResultVO.success("删除小说成功");
    }

    /**
     * 恢复小说
     */
    @PutMapping("/{id}/restore")
    public ResultVO restoreNovel(@PathVariable int id) {
        if (!novelService.restoreNovelById(id)) {
            throw ApiException.notFound("小说不存在或不在回收站中");
        }
        return ResultVO.success("恢复小说成功");
    }

}
