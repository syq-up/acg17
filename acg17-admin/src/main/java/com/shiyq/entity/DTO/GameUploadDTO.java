package com.shiyq.entity.DTO;

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
    private String title;
    
    /**
     * 中文名称
     */
    private String chineseTitle;
    
    /**
     * 版本号
     */
    private String version;
    
    /**
     * 游戏封面文件
     */
    private MultipartFile cover;
    
    /**
     * 游戏图标文件
     */
    private MultipartFile icon;
    
    /**
     * 游戏简介
     */
    private String description;
    
    /**
     * 游戏预览图片文件数组
     */
    private MultipartFile[] previewImages;
}