package com.shiyq.service.impl;

import com.shiyq.convert.IllustrationConvert;
import com.shiyq.entity.DO.Illustration;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.IllustrationVO;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.entity.VO.ReorderRequest;
import com.shiyq.mapper.IllustrationMapper;
import com.shiyq.mapper.UserMapper;
import com.shiyq.service.FileStorageService;
import com.shiyq.service.IllustrationService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
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
import java.util.Arrays;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.shiyq.util.ImageThumbnailUtil;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Service
public class IllustrationServiceImpl extends ServiceImpl<IllustrationMapper, Illustration>
        implements IllustrationService {

    @Value("${file.illustrationFolder}")
    private String illustrationFolder;
    @Value("${file.illustrationThumbFolder}")
    private String illustrationThumbFolder;
    @Value("${serverFileUrlPrefix}")
    private String serverFileUrlPrefix;
    @Value("${file.maxIllustrationFileSize:100MB}")
    private String maxIllustrationFileSize = "100MB";

    private IllustrationMapper illustrationMapper;
    private UserMapper userMapper;
    private FileStorageService fileStorageService;

    @Autowired
    public void setIllustrationMapper(IllustrationMapper illustrationMapper) {
        this.illustrationMapper = illustrationMapper;
    }

    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Autowired
    public void setFileStorageService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public IllustrationVO upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("插画文件不能为空");
        }
        long maxFileSize = DataSize.parse(maxIllustrationFileSize).toBytes();
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("插画文件大小不能超过 " + maxIllustrationFileSize);
        }
        int userId = UserContext.requireCurrentUserId();
        Path stagingDirectory = fileStorageService.createStagingDirectory("illustration-");
        Path uploadedFile = stagingDirectory.resolve("upload");
        Path finalOriginal = null;
        Path finalThumb = null;
        boolean originalMoved = false;
        boolean thumbMoved = false;
        boolean readyForCommit = false;
        try {
            file.transferTo(uploadedFile.toFile());
            String formatName = ImageThumbnailUtil.detectImageExtension(uploadedFile.toFile());
            String filename = NanoIdUtil.randomNanoId() + "." + formatName;
            Path stagedOriginal = stagingDirectory.resolve(filename);
            Path stagedThumb = stagingDirectory.resolve("thumb-" + filename);
            Files.move(uploadedFile, stagedOriginal);
            finalOriginal = fileStorageService.resolveManagedPath(illustrationFolder, filename);
            finalThumb = fileStorageService.resolveManagedPath(illustrationThumbFolder, filename);
            ImageThumbnailUtil.generateThumbnail(
                    stagedOriginal.toFile(), stagedThumb.toFile(), 400, formatName);

            int[] dimensions = ImageThumbnailUtil.getImageDimensions(stagedOriginal.toFile());
            double ratio = 0D;
            if (dimensions[1] > 0) {
                ratio = BigDecimal.valueOf((double) dimensions[0] / dimensions[1])
                        .setScale(6, RoundingMode.HALF_UP)
                        .doubleValue();
            }
            lockIllustrationSort(userId);
            Illustration illustration = new Illustration(filename, (int) file.getSize(), userId, ratio,
                    illustrationMapper.getMaxSortOrder(userId) + 1);
            if (illustrationMapper.insert(illustration) != 1) {
                throw new IllegalStateException("新增插画记录失败");
            }
            fileStorageService.moveIntoPlace(stagedOriginal, finalOriginal);
            originalMoved = true;
            fileStorageService.moveIntoPlace(stagedThumb, finalThumb);
            thumbMoved = true;
            fileStorageService.deleteOnRollback(Arrays.asList(finalOriginal, finalThumb));
            readyForCommit = true;
            return IllustrationConvert.INSTANCE.toVO(illustration)
                    .setUrlTiny(generateAccessUrl(illustration.getPath(), illustrationThumbFolder));
        } finally {
            fileStorageService.deleteQuietly(stagingDirectory);
            if (!readyForCommit) {
                if (originalMoved) {
                    fileStorageService.deleteQuietly(finalOriginal);
                }
                if (thumbMoved) {
                    fileStorageService.deleteQuietly(finalThumb);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public IllustrationVO insertIllustration(Illustration illustration) {
        int userId = UserContext.requireCurrentUserId();
        lockIllustrationSort(userId);
        illustration.setUserId(userId);
        // 新插画排在当前用户所有插画之前，逻辑删除记录也参与统一排序
        illustration.setSortOrder(illustrationMapper.getMaxSortOrder(userId) + 1);
        // 插入数据库
        if (illustrationMapper.insert(illustration) != 1) {
            throw new IllegalStateException("新增插画记录失败");
        }
        // 把路径修改为外网访问地址，再返回给前端
        return IllustrationConvert.INSTANCE.toVO(illustration)
                .setUrlTiny(generateAccessUrl(illustration.getPath(),
                        illustration.getSize() >= 20971520 ? "" : "!resizing-tiny"));
    }

    @Override
    public PageVO<IllustrationVO> getList(long pageNum, boolean deleted) {
        int userId = UserContext.requireCurrentUserId();
        // 默认页大小为 30
        long pageSize = 36L;
        PageVO<IllustrationVO> pageVO = new PageVO<>(pageSize, pageNum);
        // 查询插画作品列表
        List<Illustration> list = illustrationMapper.getListByCondition(userId, pageNum, pageSize,
                deleted);
        List<IllustrationVO> voList = IllustrationConvert.INSTANCE.toVOList(list);
        // 把路径修改为外网访问地址，再返回给前端
        for (int i = 0; i < list.size(); i++) {
            String path = list.get(i).getPath();
            // 缩略图和原图
            voList.get(i).setUrlTiny(generateAccessUrl(path, illustrationThumbFolder));
            voList.get(i).setUrlMiddle(generateAccessUrl(path, illustrationFolder));
        }
        pageVO.setRecords(voList);
        pageVO.setTotal(illustrationMapper.getTotalByCondition(userId, deleted));
        return pageVO;
    }

    @Override
    public IllustrationVO getRandomIllustration() {
        // 获取一条随机记录
        Illustration illustration = illustrationMapper.getRandomRecord();
        if (illustration == null) {
            return null;
        }
        // 转VO，把路径修改为外网访问地址，再返回给前端
        return IllustrationConvert.INSTANCE.toVO(illustration)
                .setUrlMiddle(generateAccessUrl(illustration.getPath(), illustrationFolder));
    }

    @Override
    public boolean deleteById(int id) {
        return illustrationMapper.deleteByIdAndUserId(id, UserContext.requireCurrentUserId()) > 0;
    }

    @Override
    public boolean restoreById(int id) {
        return illustrationMapper.restoreByIdAndUserId(id, UserContext.requireCurrentUserId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reorder(ReorderRequest reorderRequest) {
        if (reorderRequest == null || reorderRequest.getId() == null || reorderRequest.getTargetId() == null) {
            throw new IllegalArgumentException("插画ID和目标插画ID不能为空");
        }
        if (reorderRequest.getId().equals(reorderRequest.getTargetId())) {
            return true;
        }

        int userId = UserContext.requireCurrentUserId();
        lockIllustrationSort(userId);
        Illustration source = illustrationMapper.selectActiveByIdForUpdate(reorderRequest.getId(), userId);
        Illustration target = illustrationMapper.selectActiveByIdForUpdate(reorderRequest.getTargetId(), userId);
        if (source == null || target == null) {
            return false;
        }

        int oldSortOrder = source.getSortOrder();
        int targetSortOrder = target.getSortOrder();
        if (oldSortOrder == targetSortOrder) {
            return true;
        }

        if (illustrationMapper.moveSortOrderToTemporary(source.getId(), userId) != 1) {
            throw new IllegalStateException("暂存插画排序失败");
        }

        if (oldSortOrder < targetSortOrder) {
            illustrationMapper.decrementSortOrderRange(userId, oldSortOrder, targetSortOrder);
        } else {
            illustrationMapper.incrementSortOrderRange(userId, targetSortOrder, oldSortOrder);
        }

        if (illustrationMapper.updateSortOrderByIdAndUserId(
                source.getId(), targetSortOrder, userId) != 1) {
            throw new IllegalStateException("更新插画排序失败");
        }
        return true;
    }

    private void lockIllustrationSort(int userId) {
        Integer lockedUserId = userMapper.lockById(userId);
        if (lockedUserId == null || lockedUserId != userId) {
            throw new IllegalStateException("当前用户不存在");
        }
    }

    /**
     * 生成外网访问的URL
     */
    public String generateAccessUrl(String filename, String style) {
        return serverFileUrlPrefix + style + filename;
    }

}
