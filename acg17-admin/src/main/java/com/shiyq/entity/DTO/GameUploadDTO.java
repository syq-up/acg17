package com.shiyq.entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 游戏上传DTO
 *
 * @author shiyq
 * @since 2024-12-19
 */
@Data
public class GameUploadDTO {
    
    /**
     * 游戏名称
     */
    @NotBlank(message = "游戏名称不能为空")
    @Size(max = 255, message = "游戏名称不能超过255个字符")
    private String title;
    
    /**
     * 中文名称
     */
    @Size(max = 255, message = "游戏中文名称不能超过255个字符")
    private String chineseTitle;
    
    /**
     * 版本号
     */
    @Size(max = 50, message = "游戏版本不能超过50个字符")
    private String version;
    
    /**
     * 游戏封面文件
     */
    @NotNull(message = "游戏封面不能为空")
    private MultipartFile cover;
    
    /**
     * 游戏图标文件
     */
    private MultipartFile icon;
    
    /**
     * 游戏简介
     */
    @Size(max = 10000, message = "游戏简介不能超过10000个字符")
    private String description;
    
    /**
     * 游戏预览图片文件数组
     */
    @Size(max = 20, message = "游戏预览图不能超过20张")
    private MultipartFile[] previewImages;
}
