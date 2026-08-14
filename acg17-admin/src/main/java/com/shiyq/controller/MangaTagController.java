package com.shiyq.controller;

import com.shiyq.entity.DO.MangaTag;
import com.shiyq.entity.DTO.MangaTagCreateDTO;
import com.shiyq.entity.DTO.MangaTagUpdateDTO;
import com.shiyq.exception.ApiException;
import com.shiyq.service.MangaTagService;
import com.shiyq.entity.VO.ResultVO;
import com.shiyq.constant.MangaConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import com.shiyq.entity.VO.MangaTagListVO;
import com.shiyq.entity.VO.MangaTagVO;

/**
 * <p>
 * 漫画标签 前端控制器
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@RestController
@RequestMapping("/manga-tag")
public class MangaTagController {

    @Autowired
    private MangaTagService mangaTagService;

    /**
     * 获取所有标签
     * @param deleted 是否统计回收站中的漫画
     * @return 标签列表
     */
    @GetMapping("/list")
    public ResultVO getTagList(@RequestParam(defaultValue = "false") boolean deleted) {
        MangaTagListVO tags = mangaTagService.listTags(deleted);
        return ResultVO.success(tags);
    }

    /**
     * 根据ID获取标签
     * @param id 标签ID
     * @return 标签信息
     */
    @GetMapping("/{id}")
    public ResultVO getTagById(@PathVariable @Positive(message = "标签ID必须大于0") Integer id) {
        MangaTag tag = mangaTagService.getOwnedTagById(id);
        if (tag == null) {
            throw ApiException.notFound("标签不存在");
        }
        return ResultVO.success(tag);
    }

    /**
     * 新增标签
     * @param request 标签请求
     * @return 新增结果
     */
    @PostMapping
    public ResultVO addTag(@Valid @RequestBody MangaTagCreateDTO request) {
        MangaTag savedTag = mangaTagService.getOrCreateTagByNameAndCategory(
                request.getTagName(), request.getCategory());
        return ResultVO.success(savedTag);
    }

    /**
     * 更新标签
     * @param request 标签请求
     * @return 更新结果
     */
    @PutMapping
    public ResultVO updateTag(@Valid @RequestBody MangaTagUpdateDTO request) {
        boolean success = mangaTagService.updateOwnedTag(
                request.getId(), request.getTagName(), request.getCategory());
        if (success) {
            return ResultVO.success("更新成功");
        }
        throw ApiException.notFound("标签不存在");
    }

    /**
     * 删除标签
     * @param id 标签ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResultVO deleteTag(@PathVariable @Positive(message = "标签ID必须大于0") Integer id) {
        MangaTagService.DeleteResult result = mangaTagService.deleteUnusedTag(id);
        if (result == MangaTagService.DeleteResult.DELETED) {
            return ResultVO.success("删除成功");
        }
        if (result == MangaTagService.DeleteResult.IN_USE) {
            throw ApiException.conflict("标签正在被漫画引用，无法删除");
        }
        throw ApiException.notFound("标签不存在");
    }

    /**
     * 根据分类获取标签列表
     * @param category 分类标记（支持英文名称：character, male, female, mixed, other, original, artist 或数字：1-7）
     * @return 标签列表
     */
    @GetMapping("/category/{category}")
    public ResultVO getTagsByCategory(
            @PathVariable @Size(max = 16, message = "标签分类参数过长") String category) {
        // 将英文分类名称转换为数字分类
        Integer numericCategory = MangaConstant.parseCategory(category);
        if (numericCategory == null) {
            throw new IllegalArgumentException("无效的分类参数");
        }
        
        List<MangaTagVO> tags = mangaTagService.getTagsByCategory(numericCategory);
        return ResultVO.success(tags);
    }

    /**
     * 根据标签名和分类获取或创建标签
     * @param tagName 标签名
     * @param category 分类标记（支持英文名称：character, male, female, mixed, other, original, artist 或数字：1-7）
     * @return 标签信息
     */
    @PostMapping("/get-or-create-by-category")
    public ResultVO getOrCreateTagByNameAndCategory(
            @RequestParam @NotBlank(message = "标签名称不能为空")
            @Size(max = 50, message = "标签名称不能超过50个字符") String tagName,
            @RequestParam @Size(max = 16, message = "标签分类参数过长") String category) {
        // 将英文分类名称转换为数字分类
        Integer numericCategory = MangaConstant.parseCategory(category);
        if (numericCategory == null) {
            throw new IllegalArgumentException("无效的分类参数");
        }
        
        MangaTag tag = mangaTagService.getOrCreateTagByNameAndCategory(tagName, numericCategory);
        return ResultVO.success(tag);
    }
}
