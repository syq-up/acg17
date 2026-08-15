package com.shiyq.controller;

import com.shiyq.entity.VO.ResultVO;
import com.shiyq.entity.DTO.UserInfoUpdateDTO;
import com.shiyq.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

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
@RequestMapping("/user-info")
public class UserInfoController {

    private UserInfoService userInfoService;

    @Autowired
    public void setUserInfoService(UserInfoService userInfoService) {
        this.userInfoService = userInfoService;
    }

    @GetMapping("/getInfo")
    public ResultVO getInfo() {
        return ResultVO.success(userInfoService.getInfo());
    }

    @PatchMapping
    public ResultVO updateNickname(@RequestBody @Valid UserInfoUpdateDTO request) {
        return ResultVO.success(userInfoService.updateNickname(request.getNickname()));
    }

    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultVO updateAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        return ResultVO.success(userInfoService.updateAvatar(file));
    }

    @DeleteMapping("/avatar")
    public ResultVO resetAvatar() throws IOException {
        return ResultVO.success(userInfoService.resetAvatar());
    }

}
