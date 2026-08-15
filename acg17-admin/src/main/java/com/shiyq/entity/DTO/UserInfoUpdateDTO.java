package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新当前用户公开资料的请求。
 */
@Data
public class UserInfoUpdateDTO {

    @NotBlank(message = "昵称不能为空")
    @Size(max = 64, message = "昵称不能超过64个字符")
    private String nickname;
}
