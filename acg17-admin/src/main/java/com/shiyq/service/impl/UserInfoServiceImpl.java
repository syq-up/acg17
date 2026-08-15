package com.shiyq.service.impl;

import com.shiyq.entity.DO.UserInfo;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.UserInfoVO;
import com.shiyq.mapper.UserInfoMapper;
import com.shiyq.service.UserInfoService;
import com.shiyq.service.FileStorageService;
import com.shiyq.service.MediaUrlSigner;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.shiyq.util.ImageThumbnailUtil;
import com.shiyq.util.NanoIdUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Value("${file.avatarFolder:avatar/}")
    private String avatarFolder = "avatar/";
    @Value("${file.maxAvatarFileSize:5MB}")
    private String maxAvatarFileSize = "5MB";

    private UserInfoMapper userInfoMapper;
    private MediaUrlSigner mediaUrlSigner;
    private FileStorageService fileStorageService;

    @Autowired
    public void setUserInfoMapper(UserInfoMapper userInfoMapper) {
        this.userInfoMapper = userInfoMapper;
    }

    @Autowired
    public void setMediaUrlSigner(MediaUrlSigner mediaUrlSigner) {
        this.mediaUrlSigner = mediaUrlSigner;
    }

    @Autowired
    public void setFileStorageService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @Override
    public UserInfoVO getInfo() {
        UserInfoVO userInfo = userInfoMapper.selectUserInfoByUserId(UserContext.requireCurrentUserId());
        if (userInfo != null && userInfo.getAvatarUrl() != null && !userInfo.getAvatarUrl().trim().isEmpty()) {
            userInfo.setAvatarUrl(mediaUrlSigner.sign(avatarFolder, userInfo.getAvatarUrl()));
        }
        return userInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("昵称不能为空");
        }
        if (nickname.trim().length() > 64) {
            throw new IllegalArgumentException("昵称不能超过64个字符");
        }

        UserInfo userInfo = requireUserInfo();
        userInfo.setNickname(nickname.trim());
        if (userInfoMapper.updateById(userInfo) != 1) {
            throw new IllegalStateException("修改昵称失败");
        }
        return getInfo();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateAvatar(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("头像文件不能为空");
        }
        long maxFileSize = DataSize.parse(maxAvatarFileSize).toBytes();
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("头像文件大小不能超过 " + maxAvatarFileSize);
        }

        UserInfo userInfo = requireUserInfo();
        Path oldAvatarPath = resolveExistingAvatarPath(userInfo.getAvatarPath());
        Path stagingDirectory = fileStorageService.createStagingDirectory("avatar-");
        Path uploadedFile = stagingDirectory.resolve("upload");
        Path finalAvatarPath = null;
        boolean avatarMoved = false;
        boolean readyForCommit = false;
        try {
            file.transferTo(uploadedFile.toFile());
            String formatName;
            try {
                formatName = ImageThumbnailUtil.detectImageExtension(uploadedFile.toFile());
            } catch (IOException exception) {
                throw new IllegalArgumentException("头像文件不是受支持的有效图片", exception);
            }

            String filename = NanoIdUtil.randomNanoId() + "." + formatName;
            Path stagedAvatar = stagingDirectory.resolve(filename);
            Files.move(uploadedFile, stagedAvatar);

            String relativePath = UserContext.requireCurrentUserId() + "/" + filename;
            finalAvatarPath = fileStorageService.resolveManagedPath(avatarFolder, relativePath);
            userInfo.setAvatarPath(relativePath);
            if (userInfoMapper.updateById(userInfo) != 1) {
                throw new IllegalStateException("更新头像信息失败");
            }

            fileStorageService.moveIntoPlace(stagedAvatar, finalAvatarPath);
            avatarMoved = true;
            fileStorageService.deleteOnRollback(Collections.singletonList(finalAvatarPath));
            if (oldAvatarPath != null) {
                fileStorageService.deleteAfterCommit(Collections.singletonList(oldAvatarPath));
            }

            UserInfoVO result = getInfo();
            readyForCommit = true;
            return result;
        } finally {
            fileStorageService.deleteQuietly(stagingDirectory);
            if (!readyForCommit && avatarMoved) {
                fileStorageService.deleteQuietly(finalAvatarPath);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO resetAvatar() throws IOException {
        UserInfo userInfo = requireUserInfo();
        Path oldAvatarPath = resolveExistingAvatarPath(userInfo.getAvatarPath());
        if (oldAvatarPath == null) {
            return getInfo();
        }

        if (userInfoMapper.clearAvatarPath(userInfo.getUserId()) != 1) {
            throw new IllegalStateException("恢复默认头像失败");
        }
        fileStorageService.deleteAfterCommit(Collections.singletonList(oldAvatarPath));
        return getInfo();
    }

    private UserInfo requireUserInfo() {
        UserInfo userInfo = userInfoMapper.selectById(UserContext.requireCurrentUserId());
        if (userInfo == null) {
            throw new IllegalStateException("当前用户信息不存在");
        }
        return userInfo;
    }

    private Path resolveExistingAvatarPath(String avatarPath) throws IOException {
        if (avatarPath == null || avatarPath.trim().isEmpty()) {
            return null;
        }
        return fileStorageService.resolveManagedPath(avatarFolder, avatarPath);
    }
}
