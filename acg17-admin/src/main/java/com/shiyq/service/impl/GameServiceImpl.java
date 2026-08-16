package com.shiyq.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.shiyq.entity.DO.Game;
import com.shiyq.entity.DTO.GameUpdateDTO;
import com.shiyq.entity.DTO.GameUploadDTO;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.mapper.GameMapper;
import com.shiyq.service.FileStorageService;
import com.shiyq.service.GameService;
import com.shiyq.service.MediaUrlSigner;
import com.shiyq.util.DesktopIniUtil;
import com.shiyq.util.ImageConverterUtil;
import com.shiyq.util.ImageFileInspector;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 游戏信息服务实现类
 *
 * @author shiyq
 * @since 2024-12-19
 */
@Slf4j
@Service
public class GameServiceImpl extends ServiceImpl<GameMapper, Game> implements GameService {

    private static final int MAX_PREVIEW_IMAGES = 20;

    @Value("${file.gameFolder}")
    private String gameFolder;
    @Value("${file.maxGameImageFileSize:20MB}")
    private String maxGameImageFileSize = "20MB";
    private GameMapper gameMapper;
    private FileStorageService fileStorageService;
    private MediaUrlSigner mediaUrlSigner;

    @Autowired
    public void setGameMapper(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    @Autowired
    public void setFileStorageService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Autowired
    public void setMediaUrlSigner(MediaUrlSigner mediaUrlSigner) {
        this.mediaUrlSigner = mediaUrlSigner;
    }

    @Override
    public PageVO<Game> getGameList(long pageNum, String title, boolean deleted) {
        try {
            // 获取当前用户ID
            int userId = UserContext.requireCurrentUserId();
            // 设置分页参数
            long pageSize = 24;
            // 查询游戏列表
            List<Game> gameList = baseMapper.getListByCondition(userId, pageNum, pageSize, deleted, title);
            // 查询总记录数
            Long total = baseMapper.getTotalByCondition(userId, deleted, title);
            
            // 处理封面和预览图片URL
            for (Game game : gameList) {
                addAccessUrls(game);
            }
            
            // 封装返回结果
            PageVO<Game> pageVO = new PageVO<>(pageSize, pageNum);
            pageVO.setRecords(gameList);
            pageVO.setTotal(total);
            
            return pageVO;
        } catch (Exception e) {
            log.error("获取游戏列表失败", e);
            throw new RuntimeException("获取游戏列表失败");
        }
    }

    /**
     * 生成文件访问URL
     * @param path 相对路径
     * @return 完整的访问URL
     */
    private String generateAccessUrl(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        return mediaUrlSigner.sign(gameFolder, path);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addGame(GameUploadDTO gameUploadDTO) throws Exception {
        if (gameUploadDTO == null) {
            throw new IllegalArgumentException("游戏信息不能为空");
        }
        String title = normalizeRequiredText(gameUploadDTO.getTitle(), "游戏名称", 255);
        String chineseTitle = normalizeOptionalText(
                gameUploadDTO.getChineseTitle(), "游戏中文名称", 255);
        String version = normalizeOptionalText(gameUploadDTO.getVersion(), "游戏版本", 50);
        String description = normalizeOptionalText(gameUploadDTO.getDescription(), "游戏简介", 10000);
        MultipartFile coverFile = gameUploadDTO.getCover();
        validateImageFile(coverFile, "游戏封面", true);
        MultipartFile iconFile = gameUploadDTO.getIcon();
        validateImageFile(iconFile, "游戏图标", false);
        MultipartFile[] previewFiles = gameUploadDTO.getPreviewImages();
        if (previewFiles != null && previewFiles.length > MAX_PREVIEW_IMAGES) {
            throw new IllegalArgumentException("游戏预览图不能超过20张");
        }
        if (previewFiles != null) {
            for (MultipartFile previewFile : previewFiles) {
                validateImageFile(previewFile, "游戏预览图", true);
            }
        }

        Path stagingDirectory = fileStorageService.createStagingDirectory("game-");
        Path finalGameDirectory = null;
        boolean gameDirectoryMoved = false;
        boolean readyForCommit = false;
        try {
            Game game = new Game();
            game.setTitle(title);
            game.setChineseTitle(chineseTitle);
            game.setVersion(version);
            game.setDescription(description);
            game.setUserId(UserContext.requireCurrentUserId());
            game.setCreateTime(LocalDateTime.now());
            game.setUpdateTime(LocalDateTime.now());
            game.setDeleted(false);
            game.setFavorite(false);
            
            if (gameMapper.insert(game) != 1) {
                throw new RuntimeException("保存游戏到数据库失败");
            }

            String gameSubPath = String.valueOf(game.getId());
            Path stagedGameDirectory = stagingDirectory.resolve(gameSubPath);
            Files.createDirectories(stagedGameDirectory);
            finalGameDirectory = fileStorageService.resolveManagedPath(gameFolder, gameSubPath);

            File gameDir = stagedGameDirectory.toFile();
            File gameFileDir = stagedGameDirectory.resolve("game").toFile();
            Files.createDirectories(gameFileDir.toPath());
            
            // 处理游戏封面
            String coverFileName = storeImage(coverFile, stagedGameDirectory, "cover", "游戏封面");
            String coverPath = gameSubPath + "/" + coverFileName;
            
            // 处理游戏图标
            String iconPath = null;
            String iconFileName = null;
            if (iconFile != null && !iconFile.isEmpty()) {
                String originalFilename = iconFile.getOriginalFilename();
                iconFileName = "icon.ico";
                iconPath = gameSubPath + "/" + iconFileName;
                File iconDestFile = new File(gameDir, iconFileName);

                Path tempIcon = Files.createTempFile(stagedGameDirectory, "icon-", ".tmp");
                iconFile.transferTo(tempIcon.toFile());
                try {
                    if (ImageConverterUtil.isIcoFile(tempIcon.toFile())) {
                        Files.move(tempIcon, iconDestFile.toPath());
                    } else {
                        detectImageExtension(tempIcon.toFile(), "游戏图标");
                        ImageConverterUtil.convertToIco(tempIcon.toFile(), iconDestFile, 32);
                        log.info("成功将图标转换为ICO格式: {}", originalFilename);
                    }
                } finally {
                    fileStorageService.deleteQuietly(tempIcon);
                }
            }

            // 处理文件夹自定义名称和图标（使用windows的desktop.ini文件）
            if (iconFileName != null) {
                // 确定显示名称（优先使用中文名称，否则使用title）
                String displayName = chineseTitle;
                if (displayName == null) {
                    displayName = title;
                }
                // 为“game文件夹”、“game文件文件夹”夹创建desktop.ini
                DesktopIniUtil.createDesktopIni(gameDir.getAbsolutePath(), iconFileName, displayName);
                DesktopIniUtil.createDesktopIni(gameFileDir.getAbsolutePath(), displayName);
            }
            
            // 处理游戏预览图片
            List<String> previewImagePaths = new ArrayList<>();
            if (previewFiles != null && previewFiles.length > 0) {
                for (int i = 0; i < previewFiles.length; i++) {
                    MultipartFile previewFile = previewFiles[i];
                    String previewFileName = storeImage(
                            previewFile, stagedGameDirectory, "preview_" + (i + 1), "游戏预览图");
                    previewImagePaths.add(gameSubPath + "/" + previewFileName);
                }
            }

            // 更新游戏信息
            game.setCover(coverPath);
            game.setIcon(iconPath);
            game.setPreviewImages(previewImagePaths);
            if (gameMapper.updateById(game) != 1) {
                throw new RuntimeException("更新游戏文件路径失败");
            }

            fileStorageService.moveIntoPlace(stagedGameDirectory, finalGameDirectory);
            gameDirectoryMoved = true;
            fileStorageService.deleteOnRollback(Collections.singletonList(finalGameDirectory));
            readyForCommit = true;
            return game.getTitle();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("添加游戏失败: {}", e.getMessage(), e);
            throw new RuntimeException("添加游戏失败: " + e.getMessage(), e);
        } finally {
            fileStorageService.deleteQuietly(stagingDirectory);
            if (gameDirectoryMoved && !readyForCommit) {
                fileStorageService.deleteQuietly(finalGameDirectory);
            }
        }
    }

    @Override
    public Game getGameById(Integer id) {
        Game game = gameMapper.selectByIdAndUserId(id, UserContext.requireCurrentUserId());
        if (game != null) {
            addAccessUrls(game);
        }
        return game;
    }

    private String storeImage(MultipartFile image, Path directory, String baseName,
                              String label) throws IOException {
        Path uploadedFile = Files.createTempFile(directory, "image-", ".tmp");
        try {
            image.transferTo(uploadedFile.toFile());
            String extension = detectImageExtension(uploadedFile.toFile(), label);
            String filename = baseName + "." + extension;
            Files.move(uploadedFile, directory.resolve(filename));
            return filename;
        } finally {
            fileStorageService.deleteQuietly(uploadedFile);
        }
    }

    @Override
    public boolean updateGame(Integer id, GameUpdateDTO gameUpdateDTO) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("游戏ID必须大于0");
        }
        if (gameUpdateDTO == null) {
            throw new IllegalArgumentException("游戏信息不能为空");
        }
        Game game = new Game();
        game.setTitle(normalizeRequiredText(gameUpdateDTO.getTitle(), "游戏名称", 255));
        game.setChineseTitle(normalizeOptionalText(
                gameUpdateDTO.getChineseTitle(), "游戏中文名称", 255));
        game.setVersion(normalizeOptionalText(gameUpdateDTO.getVersion(), "游戏版本", 50));
        game.setDescription(normalizeOptionalText(
                gameUpdateDTO.getDescription(), "游戏简介", 10000));
        game.setUpdateTime(LocalDateTime.now());
        LambdaUpdateWrapper<Game> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Game::getId, id)
                     .eq(Game::getUserId, UserContext.requireCurrentUserId())
                     .eq(Game::getDeleted, false);
        return gameMapper.update(game, updateWrapper) > 0;
    }

    @Override
    public boolean deleteGame(Integer id) {
        try {
            int count = gameMapper.deleteByIdAndUserId(id, UserContext.requireCurrentUserId());
            return count > 0;
        } catch (Exception e) {
            log.error("删除游戏失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean restoreGame(Integer id) {
        try {
            int count = gameMapper.restoreByIdAndUserId(id, UserContext.requireCurrentUserId());
            return count > 0;
        } catch (Exception e) {
            log.error("恢复游戏失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean updateFavorite(Integer id, boolean favorite) {
        try {
            int count = gameMapper.updateFavoriteByIdAndUserId(id, favorite, UserContext.requireCurrentUserId());
            return count > 0;
        } catch (Exception e) {
            log.error("更新收藏状态失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Game getRandomGame() {
        Game game = gameMapper.getRandomRecord(UserContext.requireCurrentUserId());
        if (game == null) {
            return null;
        }
        addAccessUrls(game);
        return game;
    }

    private void addAccessUrls(Game game) {
        game.setCover(generateAccessUrl(game.getCover()));
        game.setIcon(generateAccessUrl(game.getIcon()));
        if (game.getPreviewImages() != null && !game.getPreviewImages().isEmpty()) {
            List<String> processedPreviewImages = new ArrayList<>();
            for (String previewImage : game.getPreviewImages()) {
                processedPreviewImages.add(generateAccessUrl(previewImage));
            }
            game.setPreviewImages(processedPreviewImages);
        }
    }

    private void validateImageFile(MultipartFile file, String label, boolean required) {
        if (file == null || file.isEmpty()) {
            if (required || file != null) {
                throw new IllegalArgumentException(label + "不能为空");
            }
            return;
        }
        long maxBytes = DataSize.parse(maxGameImageFileSize).toBytes();
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(label + "大小不能超过 " + maxGameImageFileSize);
        }
    }

    private String detectImageExtension(File imageFile, String label) {
        try {
            return ImageFileInspector.inspect(imageFile).extension();
        } catch (IOException exception) {
            throw new IllegalArgumentException(label + "不是受支持的有效图片", exception);
        }
    }

    private String normalizeRequiredText(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, String label, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + "不能超过" + maxLength + "个字符");
        }
        return normalized;
    }

}
