package com.shiyq.controller;

import com.shiyq.entity.VO.ResultVO;
import com.shiyq.service.NovelTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/novel-tag")
public class NovelTagController {

    private final NovelTagService novelTagService;

    public NovelTagController(NovelTagService novelTagService) {
        this.novelTagService = novelTagService;
    }

    @GetMapping("/getList")
    public ResultVO getList() {
        return ResultVO.success(novelTagService.listAllTags());
    }
}
