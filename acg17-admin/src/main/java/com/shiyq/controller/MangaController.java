package com.shiyq.controller;

import com.shiyq.entity.DTO.MangaChapterUploadDTO;
import com.shiyq.entity.DTO.MangaUpdateDTO;
import com.shiyq.entity.DTO.MangaUploadDTO;
import com.shiyq.entity.VO.MangaChapterVO;
import com.shiyq.entity.VO.MangaVO;
import com.shiyq.entity.VO.MangaDetailVO;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.exception.ApiException;
import com.shiyq.service.MangaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 漫画 前端控制器
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@RestController
@RequestMapping("/manga")
public class MangaController {

    @Autowired
    private MangaService mangaService;

    /**
     * 分页获取漫画列表
     * @param pageNum 页码
     * @param deleted 是否查询已删除的记录
     * @param author 作者（模糊查询）
     * @param title 标题（模糊查询，同时查询title和chinese_title）
     * @param tagId 标签ID
     * @return 分页结果
     */
    @GetMapping("/list")
    public ResultVO getList(@RequestParam(defaultValue = "1") long pageNum,
                           @RequestParam(defaultValue = "false") boolean deleted,
                           @RequestParam(required = false) String author,
                           @RequestParam(required = false) String title,
                           @RequestParam(required = false) Integer tagId) {
        PageVO<MangaVO> pageVO = mangaService.getList(pageNum, deleted, author, title, tagId);
        return ResultVO.success(pageVO);
    }

    /**
     * 根据ID获取漫画详情
     * @param id 漫画ID
     * @return 漫画详情
     */
    @GetMapping("/{id}")
    public ResultVO getMangaById(@PathVariable long id) {
        MangaDetailVO mangaDetailVO = mangaService.getMangaById(id);
        if (mangaDetailVO == null) {
            throw ApiException.notFound("漫画不存在");
        }
        return ResultVO.success(mangaDetailVO);
    }

    /**
     * 新增漫画
     * @param mangaUploadDTO 漫画上传DTO
     * @return 漫画详情
     */
    @PostMapping("/addManga")
    public ResponseEntity<ResultVO> addManga(@ModelAttribute MangaUploadDTO mangaUploadDTO) throws Exception {
        String result = mangaService.addManga(mangaUploadDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultVO.created("新增成功", result));
    }

    /**
     * 为漫画追加一个章节。
     * @param id 漫画ID
     * @param mangaChapterUploadDTO 章节标题和ZIP文件
     * @return 新章节信息
     */
    @PostMapping("/{id}/chapters")
    public ResponseEntity<ResultVO> addMangaChapter(
            @PathVariable long id,
            @ModelAttribute MangaChapterUploadDTO mangaChapterUploadDTO) {
        MangaChapterVO chapter = mangaService.addMangaChapter(id, mangaChapterUploadDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResultVO.created("新增章节成功", chapter));
    }

    /**
     * 更新漫画
     * @param id 漫画ID
     * @param mangaUpdateDTO 可更新的漫画信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResultVO updateManga(@PathVariable long id, @RequestBody MangaUpdateDTO mangaUpdateDTO) {
        if (!mangaService.updateManga(id, mangaUpdateDTO)) {
            throw ApiException.notFound("漫画不存在或已删除");
        }
        return ResultVO.success("更新成功");
    }

    /**
     * 删除漫画
     * @param id 漫画ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResultVO deleteManga(@PathVariable long id) {
        if (!mangaService.deleteMangaById(id)) {
            throw ApiException.notFound("漫画不存在或已删除");
        }
        return ResultVO.success("删除成功");
    }

    /**
     * 恢复漫画
     * @param id 漫画ID
     * @return 恢复结果
     */
    @PutMapping("/{id}/restore")
    public ResultVO restoreManga(@PathVariable long id) {
        if (!mangaService.restoreMangaById(id)) {
            throw ApiException.notFound("漫画不存在或不在回收站中");
        }
        return ResultVO.success("恢复成功");
    }

    /**
     * 更新漫画收藏状态
     * @param id 漫画ID
     * @param favorite 收藏状态
     * @return 更新结果
     */
    @PutMapping("/{id}/favorite")
    public ResultVO updateFavoriteStatus(@PathVariable long id, 
                                         @RequestParam boolean favorite) {
        if (!mangaService.updateFavoriteStatus(id, favorite)) {
            throw ApiException.notFound("漫画不存在或已删除");
        }
        return ResultVO.success("收藏状态更新成功");
    }

    /**
     * 为漫画添加标签
     * @param id 漫画ID
     * @param tagId 标签ID
     * @return 添加结果
     */
    @PostMapping("/{id}/tags")
    public ResultVO addTagToManga(@PathVariable long id,
                                 @RequestParam Integer tagId) {
        if (!mangaService.addTagToManga(id, tagId)) {
            throw ApiException.notFound("漫画或标签不存在");
        }
        return ResultVO.success("标签添加成功");
    }

    /**
     * 从漫画中删除标签
     * @param id 漫画ID
     * @param tagId 标签ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}/tags")
    public ResultVO removeTagFromManga(@PathVariable long id,
                                      @RequestParam Integer tagId) {
        if (!mangaService.removeTagFromManga(id, tagId)) {
            throw ApiException.notFound("漫画或标签不存在");
        }
        return ResultVO.success("标签删除成功");
    }

    /**
     * 删除漫画页面
     * @param mangaId 漫画ID
     * @param chapterId 章节ID
     * @param pageNum 页面编号
     * @return 删除结果
     */
    @DeleteMapping("/delete/page")
    public ResultVO deleteMangaPage(@RequestParam int mangaId,
                                    @RequestParam int chapterId,
                                    @RequestParam int pageNum) {
        if (!mangaService.realDeleteMangaPage(mangaId, chapterId, pageNum)) {
            throw ApiException.notFound("漫画页面不存在");
        }
        return ResultVO.success("页面删除成功");
    }

     /**
     * 随机获取一个漫画
     * @return 随机漫画
     */
    @GetMapping("/random")
    public ResultVO getRandomManga() {
        MangaDetailVO mangaDetailVO = mangaService.getRandomManga();
        if (mangaDetailVO == null) {
            throw ApiException.notFound("没有漫画可供随机获取");
        }
        return ResultVO.success(mangaDetailVO);
    }

}
