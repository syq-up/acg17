package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改当前用户密码的请求。
 */
@Data
public class ChangePasswordDTO {

    @NotBlank(message = "当前密码不能为空")
    @Size(max = 72, message = "密码长度不能超过72个字符")
    private String currentPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 72, message = "密码长度必须在8到72个字符之间")
    private String newPassword;
}
