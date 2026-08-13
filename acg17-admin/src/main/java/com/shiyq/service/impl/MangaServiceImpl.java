package com.shiyq.service.impl;

import com.shiyq.entity.DO.Manga;
import com.shiyq.entity.DO.MangaTag;
import com.shiyq.entity.DTO.MangaChapterUploadDTO;
import com.shiyq.entity.DTO.MangaUpdateDTO;
import com.shiyq.entity.DTO.MangaUploadDTO;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.MangaChapterVO;
import com.shiyq.entity.VO.MangaTagVO;
import com.shiyq.entity.VO.MangaDetailVO;
import com.shiyq.mapper.MangaMapper;
import com.shiyq.mapper.MangaTagRelationMapper;
import com.shiyq.service.FileStorageService;
import com.shiyq.service.MediaUrlSigner;
import com.shiyq.service.MangaService;
import com.shiyq.service.MangaTagService;
import com.shiyq.entity.VO.PageVO;
import com.shiyq.entity.VO.MangaVO;
import com.shiyq.constant.MangaConstant;
import com.shiyq.convert.MangaConvert;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.unit.DataSize;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.util.List;
import java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import com.shiyq.util.ImageThumbnailUtil;

import java.lang.SuppressWarnings;

/**
 * <p>
 * 漫画 服务实现类
 * </p>
 *
 * @author shiyq
 * @since 2022-01-19
 */
@Service
public class MangaServiceImpl extends ServiceImpl<MangaMapper, Manga> implements MangaService {

    private static final int MAX_ARCHIVE_PATH_DEPTH = 4;
    private static final int MAX_ARCHIVE_PATH_LENGTH = 1024;
    private static final int MAX_ARCHIVE_NAME_LENGTH = 255;
    private static final int MAX_TAGS = 100;

    private MangaMapper mangaMapper;
    private MangaTagRelationMapper mangaTagRelationMapper;
    private MangaTagService mangaTagService;
    private FileStorageService fileStorageService;
    private MediaUrlSigner mediaUrlSigner;
    
    @Value("${file.mangaFolder}")
    private String mangaFolder;
    
    @Value("${file.mangaZip.maxEntries:5000}")
    private int maxZipEntries = 5000;

    @Value("${file.mangaZip.maxEntrySize:100MB}")
    private String maxZipEntrySize = "100MB";

    @Value("${file.mangaZip.maxExtractedSize:1GB}")
    private String maxZipExtractedSize = "1GB";

    @Autowired
    public void setMangaMapper(MangaMapper mangaMapper) {
        this.mangaMapper = mangaMapper;
    }

    @Autowired
    public void setMangaTagRelationMapper(MangaTagRelationMapper mangaTagRelationMapper) {
        this.mangaTagRelationMapper = mangaTagRelationMapper;
    }
    
    @Autowired
    public void setMangaTagService(MangaTagService mangaTagService) {
        this.mangaTagService = mangaTagService;
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
    public PageVO<MangaVO> getList(long pageNum, boolean deleted, String author, String title,
                                  Integer tagId) {
        if (pageNum <= 0) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (tagId != null && tagId <= 0) {
            throw new IllegalArgumentException("标签ID必须大于0");
        }
        author = normalizeOptionalText(author, "作者", 100);
        title = normalizeOptionalText(title, "标题", 255);
        int userId = UserContext.requireCurrentUserId();
        // 默认页大小为 30
        PageVO<MangaVO> pageVO = new PageVO<>(30L, pageNum);
        // 查询漫画作品列表
        List<Manga> list = mangaMapper.getListByCondition(userId, pageNum, 30L, deleted,
                author, title, tagId);
        
        // 转换为VO并处理cover路径和favorite字段
        List<MangaVO> mangaVOList = MangaConvert.INSTANCE.toMangaVOList(list);
        for (int i = 0; i < mangaVOList.size(); i++) {
            MangaVO mangaVO = mangaVOList.get(i);
            Manga manga = list.get(i);
            
            // 生成cover外网URL
            mangaVO.setCover(generateAccessUrl(manga.getCover()));
        }
        
        pageVO.setRecords(mangaVOList);
        // 查询总记录数
        pageVO.setTotal(mangaMapper.getTotalByCondition(userId, deleted,
                author, title, tagId));
        return pageVO;
    }

    /**
     * 生成外网访问的URL
     */
    public String generateAccessUrl(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        return mediaUrlSigner.sign(mangaFolder, path);
    }

    @Override
    public MangaDetailVO getMangaById(long id) {
        // 查询漫画详情
        Manga manga = mangaMapper.getMangaDetailById(id, UserContext.requireCurrentUserId());
        if (manga == null) {
            return null;
        }

        // 转换为MangaDetailVO
        MangaDetailVO mangaDetailVO = MangaConvert.INSTANCE.toMangaDetailVO(manga);
        
        // 封面URL、喜欢字段处理
        mangaDetailVO.setCover(generateAccessUrl(manga.getCover()));
        
        // pages字段处理
        if (manga.getPages() != null) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Map<String, Object>> pages = objectMapper.readValue(
                        manga.getPages(), new TypeReference<List<Map<String, Object>>>() { });
                addPageAccessUrls(pages);

                mangaDetailVO.setPages(pages);
            } catch (JacksonException e) {
                e.printStackTrace();
            }
        } else {
            mangaDetailVO.setPages(new ArrayList<>());
        }
        
        populateTagGroups(mangaDetailVO, mangaTagService.getTagsByMangaId(manga.getId()));
        
        return mangaDetailVO;
    }

    private void addPageAccessUrls(List<Map<String, Object>> chapters) {
        if (chapters == null) {
            return;
        }
        for (Map<String, Object> chapter : chapters) {
            if (chapter == null) {
                continue;
            }
            Object pageListValue = chapter.get("pagelist");
            if (!(pageListValue instanceof List)) {
                continue;
            }
            for (Object pageValue : (List<?>) pageListValue) {
                if (!(pageValue instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> page = (Map<String, Object>) pageValue;
                Object pathValue = page.get("path");
                if (pathValue instanceof String) {
                    page.put("path", generateAccessUrl((String) pathValue));
                }
            }
        }
    }

    private void populateTagGroups(MangaDetailVO detail, List<MangaTagVO> tags) {
        detail.setCharacterTags(new ArrayList<>());
        detail.setMaleTags(new ArrayList<>());
        detail.setFemaleTags(new ArrayList<>());
        detail.setMixedTags(new ArrayList<>());
        detail.setOtherTags(new ArrayList<>());
        detail.setOriginalTags(new ArrayList<>());
        for (MangaTagVO tag : tags) {
            if (MangaConstant.TAG_CATEGORY_CHARACTER.equals(tag.getCategory())) {
                detail.getCharacterTags().add(tag);
            } else if (MangaConstant.TAG_CATEGORY_MALE.equals(tag.getCategory())) {
                detail.getMaleTags().add(tag);
            } else if (MangaConstant.TAG_CATEGORY_FEMALE.equals(tag.getCategory())) {
                detail.getFemaleTags().add(tag);
            } else if (MangaConstant.TAG_CATEGORY_MIXED.equals(tag.getCategory())) {
                detail.getMixedTags().add(tag);
            } else if (MangaConstant.TAG_CATEGORY_OTHER.equals(tag.getCategory())) {
                detail.getOtherTags().add(tag);
            } else if (MangaConstant.TAG_CATEGORY_ORIGINAL.equals(tag.getCategory())) {
                detail.getOriginalTags().add(tag);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addManga(MangaUploadDTO mangaUploadDTO) throws Exception {
        if (mangaUploadDTO == null) {
            throw new IllegalArgumentException("漫画信息不能为空");
        }
        String title = normalizeRequiredText(mangaUploadDTO.getTitle(), "漫画标题", 255);
        String chineseTitle = normalizeOptionalText(
                mangaUploadDTO.getChineseTitle(), "漫画中文标题", 255);
        String description = normalizeOptionalText(
                mangaUploadDTO.getDescription(), "漫画简介", 10000);
        String author = normalizeOptionalText(mangaUploadDTO.getAuthor(), "漫画作者", 100);
        String tagsJson = mangaUploadDTO.getTags();
        if (tagsJson != null && tagsJson.length() > 16384) {
            throw new IllegalArgumentException("漫画标签数据过长");
        }
        int userId = UserContext.requireCurrentUserId();
        MultipartFile uploadFile = mangaUploadDTO.getFile();
        if (uploadFile == null || uploadFile.isEmpty()) {
            throw new IllegalArgumentException("漫画压缩包不能为空");
        }
        String originalFilename = uploadFile.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("文件格式错误，必须为ZIP文件");
        }
        Path stagingDirectory = null;
        Path finalMangaDirectory = null;
        boolean mangaDirectoryMoved = false;
        boolean readyForCommit = false;
        try {
            // 创建Manga对象
            Manga manga = new Manga();
            manga.setUserId(userId);
            manga.setTitle(title);
            manga.setChineseTitle(chineseTitle);
            manga.setDescription(description);
            manga.setAuthor(author);
            ObjectMapper objectMapper = new ObjectMapper();
            Set<Integer> tagIds = resolveTagIds(tagsJson, objectMapper);
            
            // 先保存到数据库获取ID
            int insertResult = mangaMapper.insert(manga);
            if (insertResult != 1) {
                throw new RuntimeException("保存漫画到数据库失败");
            }
            for (Integer tagId : tagIds) {
                if (mangaTagRelationMapper.insertRelation(manga.getId(), tagId, userId) < 0) {
                    throw new IllegalStateException("保存漫画标签关联失败");
                }
            }
            
            // 处理文件上传
            MultipartFile file = mangaUploadDTO.getFile();
            if (file != null && !file.isEmpty()) {
                // 检查文件是否是zip文件
                stagingDirectory = fileStorageService.createStagingDirectory("manga-");
                File tempZipFile = stagingDirectory.resolve("upload.zip").toFile();
                file.transferTo(tempZipFile);
                try {
                    processZipFile(tempZipFile, String.valueOf(manga.getId()), stagingDirectory.toString());
                    Path stagedMangaDirectory = stagingDirectory.resolve(String.valueOf(manga.getId()));
                    // 设置漫画封面路径和大小
                    manga.setCover(manga.getId() + "/cover.jpg");
                    long storedSize = FileUtils.sizeOfDirectory(stagedMangaDirectory.toFile());
                    manga.setSize(storedSize);
                    
                    // 生成pages参数
                    String pages = generatePagesJson(stagedMangaDirectory.toFile(), String.valueOf(manga.getId()));
                    manga.setPages(pages);
                    
                    // 更新数据库中的封面路径、大小和pages
                    if (mangaMapper.updateById(manga) != 1) {
                        throw new IllegalStateException("更新漫画文件信息失败");
                    }
                    finalMangaDirectory = fileStorageService.resolveManagedPath(
                            mangaFolder, String.valueOf(manga.getId()));
                    fileStorageService.moveIntoPlace(stagedMangaDirectory, finalMangaDirectory);
                    mangaDirectoryMoved = true;
                    fileStorageService.deleteOnRollback(Collections.singletonList(finalMangaDirectory));
                    readyForCommit = true;
                } finally {
                    fileStorageService.deleteQuietly(tempZipFile.toPath());
                }
            }

            return manga.getTitle(); // 成功
            
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("添加漫画失败: " + e.getMessage(), e);
        } finally {
            fileStorageService.deleteQuietly(stagingDirectory);
            if (mangaDirectoryMoved && !readyForCommit) {
                fileStorageService.deleteQuietly(finalMangaDirectory);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MangaChapterVO addMangaChapter(long mangaId,
                                          MangaChapterUploadDTO mangaChapterUploadDTO) {
        if (mangaId <= 0L) {
            throw new IllegalArgumentException("漫画ID无效");
        }
        if (mangaChapterUploadDTO == null) {
            throw new IllegalArgumentException("章节信息不能为空");
        }
        String chapterTitle = mangaChapterUploadDTO.getTitle() == null
                ? "" : mangaChapterUploadDTO.getTitle().trim();
        if (chapterTitle.isEmpty()) {
            throw new IllegalArgumentException("章节标题不能为空");
        }
        if (chapterTitle.length() > 150) {
            throw new IllegalArgumentException("章节标题不能超过150个字符");
        }
        MultipartFile uploadFile = mangaChapterUploadDTO.getFile();
        if (uploadFile == null || uploadFile.isEmpty()) {
            throw new IllegalArgumentException("章节压缩包不能为空");
        }
        String originalFilename = uploadFile.getOriginalFilename();
        if (originalFilename == null
                || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("文件格式错误，必须为ZIP文件");
        }

        Path stagingDirectory = null;
        Path finalChapterDirectory = null;
        boolean chapterDirectoryMoved = false;
        boolean readyForCommit = false;
        try {
            stagingDirectory = fileStorageService.createStagingDirectory("manga-chapter-");
            File tempZipFile = stagingDirectory.resolve("upload.zip").toFile();
            uploadFile.transferTo(tempZipFile);
            Path stagedChapterDirectory = stagingDirectory.resolve("chapter");
            processChapterZipFile(tempZipFile, stagedChapterDirectory.toFile());

            int userId = UserContext.requireCurrentUserId();
            Manga manga = mangaMapper.selectOwnedMangaByIdForUpdate(mangaId, userId);
            if (manga == null) {
                throw new IllegalArgumentException("漫画不存在或已删除");
            }

            ObjectMapper objectMapper = new ObjectMapper();
            List<Map<String, Object>> chapters = parseChapterData(manga.getPages(), objectMapper);
            int chapterNumber = nextChapterNumber(chapters);
            List<Map<String, Object>> pageList = buildChapterPageList(
                    stagedChapterDirectory.toFile(), manga.getId(), chapterNumber);

            Map<String, Object> chapter = new LinkedHashMap<>();
            chapter.put("chapter", chapterNumber);
            chapter.put("title", chapterTitle);
            chapter.put("pagelist", pageList);
            chapters.add(chapter);

            String pages = objectMapper.writeValueAsString(chapters);

            finalChapterDirectory = fileStorageService.resolveManagedPath(
                    mangaFolder, manga.getId() + "/" + chapterNumber);
            if (!Files.isDirectory(finalChapterDirectory.getParent())) {
                throw new IOException("漫画文件目录不存在");
            }
            fileStorageService.moveIntoPlace(stagedChapterDirectory, finalChapterDirectory);
            chapterDirectoryMoved = true;
            fileStorageService.deleteOnRollback(Collections.singletonList(finalChapterDirectory));
            long totalSize = FileUtils.sizeOfDirectory(finalChapterDirectory.getParent().toFile());

            if (mangaMapper.updateChapterData(mangaId, userId, pages, totalSize) != 1) {
                throw new IllegalStateException("更新漫画章节数据失败");
            }
            readyForCommit = true;

            MangaChapterVO result = new MangaChapterVO();
            result.setChapter(chapterNumber);
            result.setTitle(chapterTitle);
            result.setPageCount(pageList.size());
            return result;
        } catch (IOException e) {
            throw new RuntimeException("章节文件处理失败: " + e.getMessage(), e);
        } finally {
            fileStorageService.deleteQuietly(stagingDirectory);
            if (chapterDirectoryMoved && !readyForCommit) {
                fileStorageService.deleteQuietly(finalChapterDirectory);
            }
        }
    }

    @Override
    public boolean updateManga(long id, MangaUpdateDTO mangaUpdateDTO) {
        if (id <= 0) {
            throw new IllegalArgumentException("漫画ID必须大于0");
        }
        if (mangaUpdateDTO == null) {
            throw new IllegalArgumentException("漫画信息不能为空");
        }
        Manga manga = new Manga();
        manga.setTitle(normalizeRequiredText(mangaUpdateDTO.getTitle(), "漫画标题", 255));
        manga.setChineseTitle(normalizeOptionalText(
                mangaUpdateDTO.getChineseTitle(), "漫画中文标题", 255));
        manga.setDescription(normalizeOptionalText(
                mangaUpdateDTO.getDescription(), "漫画简介", 10000));
        manga.setAuthor(normalizeOptionalText(mangaUpdateDTO.getAuthor(), "漫画作者", 100));

        LambdaUpdateWrapper<Manga> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Manga::getId, id)
                .eq(Manga::getUserId, UserContext.requireCurrentUserId())
                .eq(Manga::getDeleted, false);
        return mangaMapper.update(manga, updateWrapper) > 0;
    }
    
    private Set<Integer> resolveTagIds(String tagsJson, ObjectMapper objectMapper) {
        Set<Integer> tagIds = new LinkedHashSet<>();
        if (tagsJson == null || tagsJson.trim().isEmpty()) {
            return tagIds;
        }
        MangaTagVO[] tags;
        try {
            tags = objectMapper.readValue(tagsJson, MangaTagVO[].class);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("漫画标签数据格式错误", exception);
        }
        if (tags == null) {
            throw new IllegalArgumentException("漫画标签数据格式错误");
        }
        if (tags.length > MAX_TAGS) {
            throw new IllegalArgumentException("漫画标签不能超过100个");
        }
        for (MangaTagVO tagVO : tags) {
            if (tagVO == null) {
                throw new IllegalArgumentException("漫画标签不能为空");
            }
            if (!MangaConstant.isValidCategory(tagVO.getCategory())) {
                throw new IllegalArgumentException("标签分类无效");
            }
            MangaTag tag;
            if (tagVO.getTagId() != null) {
                if (tagVO.getTagId() <= 0) {
                    throw new IllegalArgumentException("标签ID必须大于0");
                }
                tag = mangaTagService.getOwnedTagById(tagVO.getTagId());
                if (tag == null || !tagVO.getCategory().equals(tag.getCategory())) {
                    throw new IllegalArgumentException("标签不存在或分类不匹配");
                }
            } else {
                tag = mangaTagService.getOrCreateTagByNameAndCategory(
                        tagVO.getTagName(), tagVO.getCategory());
            }
            tagIds.add(tag.getId());
        }
        return tagIds;
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
    
    /**
     * 处理ZIP文件解压和文件结构
     * @param zipFile ZIP文件
     * @param dirName 漫画文件夹名
     * @param targetBasePath 目标基础路径
     */
    private void processZipFile(File zipFile, String dirName, String targetBasePath) throws Exception {
        try {
            validateZipSignature(zipFile);
            // 创建临时解压目录
            File targetBaseDirectory = new File(targetBasePath);
            Files.createDirectories(targetBaseDirectory.toPath());
            File tempExtractDir = Files.createTempDirectory(targetBaseDirectory.toPath(), "extract-").toFile();
            try {
                // 解压ZIP文件到临时目录
                extractZipFile(zipFile, tempExtractDir);
                removeIgnoredArchiveMetadata(tempExtractDir);
                // 分析文件结构并调整
                File processedDir = analyzeAndAdjustStructure(tempExtractDir);
                // 检查目标文件夹是否已存在
                File targetDir = new File(targetBasePath, dirName);
                if (targetDir.exists()) {
                    throw new IOException("漫画文件夹已存在");
                }
                // 写入磁盘
                try {
                    // 确保目标目录存在
                    Files.createDirectories(targetDir.getParentFile().toPath());
                    // 复制文件到目标位置，并重新编号第二层文件夹
                    copyDirectoryWithRenumbering(processedDir, targetDir);
                    // 生成封面缩略图
                    generateCoverThumbnail(targetDir);
                } catch (Exception e) {
                    if (targetDir.exists()) {
                        FileUtils.deleteDirectory(targetDir);
                    }
                    throw new IOException("文件写入失败："+e.getMessage(), e);
                }
            } finally {
                // 清理临时解压目录
                if (tempExtractDir.exists()) {
                    FileUtils.deleteDirectory(tempExtractDir);
                }
            }
            
        } catch (IOException e) {
            throw new IOException("文件处理失败："+e.getMessage(), e);
        }
    }

    /**
     * 解压并规范化单个章节。压缩包可以直接放图片，也可以包含最多两层外包目录，
     * 但最终只能解析出一个章节。
     */
    private void processChapterZipFile(File zipFile, File targetChapterDirectory) throws IOException {
        validateZipSignature(zipFile);
        File stagingDirectory = targetChapterDirectory.getParentFile();
        Files.createDirectories(stagingDirectory.toPath());
        File tempExtractDirectory = Files.createTempDirectory(
                stagingDirectory.toPath(), "extract-chapter-").toFile();
        try {
            extractZipFile(zipFile, tempExtractDirectory);
            removeIgnoredArchiveMetadata(tempExtractDirectory);
            File processedDirectory;
            try {
                processedDirectory = analyzeAndAdjustStructure(tempExtractDirectory);
            } catch (Exception e) {
                if (e instanceof IOException) {
                    throw (IOException) e;
                }
                throw new IOException(e.getMessage(), e);
            }
            File[] chapterDirectories = requireDirectoryItems(processedDirectory);
            if (!containsOnlyChapterDirectories(chapterDirectories)
                    || chapterDirectories.length != 1) {
                throw new IOException("一个章节压缩包只能包含一个章节");
            }
            copyChapterImages(chapterDirectories[0], targetChapterDirectory);
        } finally {
            if (tempExtractDirectory.exists()) {
                FileUtils.deleteDirectory(tempExtractDirectory);
            }
        }
    }

    private List<Map<String, Object>> parseChapterData(String pages,
                                                        ObjectMapper objectMapper) throws IOException {
        if (pages == null || pages.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> chapters = objectMapper.readValue(
                pages, new TypeReference<List<Map<String, Object>>>() { });
        if (chapters == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(chapters);
    }

    private int nextChapterNumber(List<Map<String, Object>> chapters) throws IOException {
        int maximum = 0;
        Set<Integer> chapterNumbers = new HashSet<>();
        for (Map<String, Object> chapter : chapters) {
            if (chapter == null || !(chapter.get("chapter") instanceof Number)) {
                throw new IOException("现有漫画章节数据格式错误");
            }
            Number rawValue = (Number) chapter.get("chapter");
            long value = rawValue.longValue();
            if (rawValue.doubleValue() != (double) value
                    || value <= 0L || value > Integer.MAX_VALUE) {
                throw new IOException("现有漫画章节编号无效");
            }
            int chapterNumber = (int) value;
            if (!chapterNumbers.add(chapterNumber)) {
                throw new IOException("现有漫画章节编号重复");
            }
            maximum = Math.max(maximum, chapterNumber);
        }
        if (maximum == Integer.MAX_VALUE) {
            throw new IOException("漫画章节编号已达到上限");
        }
        return maximum + 1;
    }

    private List<Map<String, Object>> buildChapterPageList(File chapterDirectory,
                                                            Integer mangaId,
                                                            int chapterNumber) throws IOException {
        File[] imageFiles = requireDirectoryItems(chapterDirectory);
        if (!containsOnlyImages(imageFiles)) {
            throw new IOException("章节目录只能包含图片");
        }
        Arrays.sort(imageFiles, (a, b) -> Integer.compare(
                pageNumberFromFilename(a), pageNumberFromFilename(b)));

        List<Map<String, Object>> pageList = new ArrayList<>();
        int pageNumber = 1;
        for (File imageFile : imageFiles) {
            Map<String, Object> page = new LinkedHashMap<>();
            page.put("page", pageNumber);
            page.put("path", mangaId + "/" + chapterNumber + "/" + imageFile.getName());
            pageList.add(page);
            pageNumber++;
        }
        return pageList;
    }
    
    /**
     * 解压ZIP文件
     */
    private void extractZipFile(File zipFile, File destDir) throws IOException {
        if (maxZipEntries <= 0) {
            throw new IOException("ZIP最大条目数配置必须大于0");
        }
        long maxEntryBytes = parsePositiveDataSize(maxZipEntrySize, "ZIP单文件解压上限");
        long maxExtractedBytes = parsePositiveDataSize(maxZipExtractedSize, "ZIP累计解压上限");
        Path destinationRoot = destDir.toPath().toAbsolutePath().normalize();
        Set<Path> extractedPaths = new HashSet<>();
        int entryCount = 0;
        int fileCount = 0;
        long totalExtractedBytes = 0L;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > maxZipEntries) {
                    throw new IOException("压缩包文件条目过多，最多允许" + maxZipEntries + "个");
                }

                Path entryPath = resolveArchiveEntry(destinationRoot, entry.getName());
                if (!extractedPaths.add(entryPath)) {
                    throw new IOException("压缩包包含重复路径: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    long declaredSize = entry.getSize();
                    if (declaredSize > maxEntryBytes) {
                        throw new IOException("压缩包内单个文件过大: " + entry.getName());
                    }
                    Files.createDirectories(entryPath.getParent());
                    long entryExtractedBytes = 0L;
                    try (OutputStream output = Files.newOutputStream(entryPath,
                            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            if (len == 0) {
                                continue;
                            }
                            if (entryExtractedBytes > maxEntryBytes - len) {
                                throw new IOException("压缩包内单个文件解压后过大: " + entry.getName());
                            }
                            if (totalExtractedBytes > maxExtractedBytes - len) {
                                throw new IOException("压缩包累计解压大小超过限制");
                            }
                            output.write(buffer, 0, len);
                            entryExtractedBytes += len;
                            totalExtractedBytes += len;
                        }
                    }
                    fileCount++;
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new IOException("解压文件失败: " + e.getMessage(), e);
        }

        if (fileCount == 0) {
            throw new IOException("压缩包中没有文件");
        }
    }

    private void validateZipSignature(File zipFile) throws IOException {
        byte[] signature = new byte[4];
        try (FileInputStream input = new FileInputStream(zipFile)) {
            if (input.read(signature) != signature.length
                    || signature[0] != 0x50 || signature[1] != 0x4B
                    || signature[2] != 0x03 || signature[3] != 0x04) {
                throw new IOException("文件内容不是有效的ZIP压缩包");
            }
        }
    }

    private long parsePositiveDataSize(String value, String propertyName) throws IOException {
        try {
            long bytes = DataSize.parse(value).toBytes();
            if (bytes <= 0L) {
                throw new IllegalArgumentException("必须大于0");
            }
            return bytes;
        } catch (IllegalArgumentException e) {
            throw new IOException(propertyName + "配置无效: " + value, e);
        }
    }

    private Path resolveArchiveEntry(Path destinationRoot, String entryName) throws IOException {
        if (entryName == null || entryName.trim().isEmpty()) {
            throw new IOException("压缩包包含空路径条目");
        }
        String normalizedName = entryName.replace('\\', '/');
        if (normalizedName.length() > MAX_ARCHIVE_PATH_LENGTH
                || normalizedName.startsWith("/")
                || normalizedName.matches("^[A-Za-z]:.*")) {
            throw new IOException("压缩包路径无效: " + entryName);
        }

        int depth = 0;
        for (String segment : normalizedName.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            if (".".equals(segment) || "..".equals(segment)
                    || segment.length() > MAX_ARCHIVE_NAME_LENGTH) {
                throw new IOException("压缩包路径无效: " + entryName);
            }
            depth++;
        }
        if (depth == 0 || depth > MAX_ARCHIVE_PATH_DEPTH) {
            throw new IOException("压缩包目录层级过深: " + entryName);
        }

        Path target = destinationRoot.resolve(normalizedName).normalize();
        if (!target.startsWith(destinationRoot)) {
            throw new IOException("压缩包条目超出目标目录: " + entryName);
        }
        String destinationPrefix = destinationRoot.toFile().getCanonicalPath() + File.separator;
        if (!target.toFile().getCanonicalPath().startsWith(destinationPrefix)) {
            throw new IOException("压缩包条目超出目标目录: " + entryName);
        }
        return target;
    }

    private void removeIgnoredArchiveMetadata(File directory) throws IOException {
        File[] children = directory.listFiles();
        if (children == null) {
            throw new IOException("无法读取解压目录: " + directory.getName());
        }
        for (File child : children) {
            if (isIgnoredArchiveItem(child.getName())) {
                FileUtils.forceDelete(child);
            } else if (child.isDirectory()) {
                removeIgnoredArchiveMetadata(child);
            }
        }
    }

    private boolean isIgnoredArchiveItem(String name) {
        String lowerName = name.toLowerCase(Locale.ROOT);
        return ".ds_store".equals(lowerName)
                || "thumbs.db".equals(lowerName)
                || "__macosx".equals(lowerName)
                || lowerName.startsWith("._")
                || lowerName.endsWith(".torrent");
    }
    
    /**
     * 分析并调整文件结构
     * - 二层：章节文件夹（层1）→ （多个）图片（层2）
     * - 三层：漫画文件夹（层1）→ （一个或多个）章节文件夹（层2）→ （多个）图片（层3）
     * - 四层：外包一层（层1）→ 漫画文件夹（层2）→ （一个或多个）章节文件夹（层3）→ （多个）图片（层4）
     * 返回漫画文件夹
     */
    private File analyzeAndAdjustStructure(File extractDir) throws Exception {
        File candidate = extractDir;
        for (int wrapperDepth = 0; wrapperDepth <= 2; wrapperDepth++) {
            File[] items = requireDirectoryItems(candidate);
            if (containsOnlyImages(items)) {
                File chapterDirectory = new File(candidate, "1");
                Files.createDirectory(chapterDirectory.toPath());
                for (File image : items) {
                    Files.move(image.toPath(), new File(chapterDirectory, image.getName()).toPath());
                }
                return candidate;
            }
            if (containsOnlyChapterDirectories(items)) {
                return candidate;
            }
            if (wrapperDepth < 2 && items.length == 1 && items[0].isDirectory()) {
                candidate = items[0];
                continue;
            }
            break;
        }
        throw new IOException("压缩包结构异常，只允许图片、章节目录及最多两层外包目录");
    }

    private File[] requireDirectoryItems(File directory) throws IOException {
        File[] items = directory.listFiles();
        if (items == null || items.length == 0) {
            throw new IOException("压缩包目录为空: " + directory.getName());
        }
        return items;
    }

    private boolean containsOnlyImages(File[] items) {
        if (items.length == 0) {
            return false;
        }
        for (File item : items) {
            if (!item.isFile() || !isImageFile(item)) {
                return false;
            }
        }
        return true;
    }

    private boolean containsOnlyChapterDirectories(File[] items) throws IOException {
        if (items.length == 0) {
            return false;
        }
        for (File item : items) {
            if (!item.isDirectory() || !containsOnlyImages(requireDirectoryItems(item))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 生成pages参数的JSON字符串
     * @param mangaDir 漫画目录
     * @param dirName 第一层文件夹名称（漫画ID）
     * @return pages的JSON字符串
     */
    private String generatePagesJson(File mangaDir, String dirName) throws IOException {
        try {
            List<Map<String, Object>> chapters = new ArrayList<>();
            
            // 获取第二层文件夹（章节文件夹）
            File[] chapterDirs = mangaDir.listFiles(File::isDirectory);
            if (chapterDirs != null) {
                // 按文件夹名称排序（现在文件夹名称是数字编号）
                Arrays.sort(chapterDirs, (a, b) -> {
                    try {
                        int numA = Integer.parseInt(a.getName());
                        int numB = Integer.parseInt(b.getName());
                        return Integer.compare(numA, numB);
                    } catch (NumberFormatException e) {
                        // 如果不是数字，按字符串排序
                        return a.getName().compareTo(b.getName());
                    }
                });
                
                for (File chapterDir : chapterDirs) {
                    Map<String, Object> chapter = new HashMap<>();
                    // chapter值使用文件夹名称（即编号）
                    int chapterNum = Integer.parseInt(chapterDir.getName());
                    chapter.put("chapter", chapterNum);
                    chapter.put("title", "第" + chapterNum + "话");
                    
                    // 获取章节中的图片文件
                    List<Map<String, Object>> pageList = new ArrayList<>();
                    File[] imageFiles = chapterDir.listFiles(file -> isImageFile(file));
                    if (imageFiles != null) {
                        Arrays.sort(imageFiles, (a, b) -> Integer.compare(
                                pageNumberFromFilename(a), pageNumberFromFilename(b)));
                        
                        int pageNum = 1;
                        for (File imageFile : imageFiles) {
                            Map<String, Object> page = new HashMap<>();
                            page.put("page", pageNum);
                            page.put("path", dirName + "/" + chapterDir.getName() + "/" + imageFile.getName());
                            pageList.add(page);
                            pageNum++;
                        }
                    }
                    
                    chapter.put("pagelist", pageList);
                    chapters.add(chapter);
                }
            }
            
            // 转换为JSON字符串
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(chapters);
        } catch (Exception e) {
            throw new IOException("生成漫画页面数据失败", e);
        }
    }
    
    /**
      * 复制目录并重新编号第二层文件夹
      * @param sourceDir 源目录
      * @param targetDir 目标目录
      */
    private void copyDirectoryWithRenumbering(File sourceDir, File targetDir) throws IOException {
        Files.createDirectories(targetDir.toPath());

        File[] chapterDirectories = requireDirectoryItems(sourceDir);
        if (!containsOnlyChapterDirectories(chapterDirectories)) {
            throw new IOException("漫画目录只能包含章节文件夹和图片");
        }
        Arrays.sort(chapterDirectories, this::compareNaturalNames);

        int chapterNumber = 1;
        for (File chapterDirectory : chapterDirectories) {
            File targetChapterDirectory = new File(targetDir, String.valueOf(chapterNumber));
            copyChapterImages(chapterDirectory, targetChapterDirectory);
            chapterNumber++;
        }
    }

    private void copyChapterImages(File sourceChapterDirectory,
                                   File targetChapterDirectory) throws IOException {
        File[] imageFiles = requireDirectoryItems(sourceChapterDirectory);
        if (!containsOnlyImages(imageFiles)) {
            throw new IOException("章节目录只能包含图片");
        }
        Arrays.sort(imageFiles, this::compareNaturalNames);
        Files.createDirectory(targetChapterDirectory.toPath());
        int pageNumber = 1;
        for (File imageFile : imageFiles) {
            String extension = ImageThumbnailUtil.detectImageExtension(imageFile);
            File targetFile = new File(targetChapterDirectory, pageNumber + "." + extension);
            FileUtils.copyFile(imageFile, targetFile);
            pageNumber++;
        }
    }

    /**
     * 生成封面缩略图
    * @param mangaDir 漫画目录
    */
    private void generateCoverThumbnail(File mangaDir) {
        try {
            // 查找封面图片文件
            File coverImage = findCoverFile(mangaDir);
            if (coverImage != null) {
                File thumbnailFile = new File(mangaDir, "cover.jpg");
                ImageThumbnailUtil.generateThumbnail(coverImage, thumbnailFile, 440, "jpg");
            } else {
                throw new Exception("未找到封面图片文件");
            }
        } catch (Exception e) {
            throw new RuntimeException("生成封面缩略图失败");
        }
    }
    
    /**
     * 查找封面图片文件
     * @param dir 漫画目录
     * @return 封面图片文件
     */
    private File findCoverFile(File dir) {
        if (dir == null) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }
        // 查找所有章节目录→按名称排序
        List<File> chapterDirs = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                chapterDirs.add(file);
            }
        }
        if (chapterDirs.isEmpty()) {
            return null;
        }
        chapterDirs.sort((a, b) -> Integer.compare(
                Integer.parseInt(a.getName()), Integer.parseInt(b.getName())));

        // 查找第一个章节目录下的所有图片文件→按名称排序
        File firstChapterDir = chapterDirs.get(0);
        File[] subFiles = firstChapterDir.listFiles();
        if (subFiles == null) {
            return null;
        }
        Arrays.sort(subFiles, (a, b) -> Integer.compare(
                pageNumberFromFilename(a), pageNumberFromFilename(b)));

        // 查找第一个图片文件
        for (File subFile : subFiles) {
            if (subFile.isFile() && isImageFile(subFile)) {
                return subFile;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为图片文件
     * @param file 文件
     * @return 是否为图片文件
     */
    private boolean isImageFile(File file) {
        try {
            ImageThumbnailUtil.detectImageExtension(file);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private int pageNumberFromFilename(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return Integer.parseInt(dotIndex < 0 ? name : name.substring(0, dotIndex));
    }

    private int compareNaturalNames(File left, File right) {
        String leftName = left.getName();
        String rightName = right.getName();
        int leftIndex = 0;
        int rightIndex = 0;
        int zeroPaddingComparison = 0;

        while (leftIndex < leftName.length() && rightIndex < rightName.length()) {
            char leftChar = leftName.charAt(leftIndex);
            char rightChar = rightName.charAt(rightIndex);
            if (isAsciiDigit(leftChar) && isAsciiDigit(rightChar)) {
                int leftEnd = digitRunEnd(leftName, leftIndex);
                int rightEnd = digitRunEnd(rightName, rightIndex);
                int leftSignificant = skipLeadingZeros(leftName, leftIndex, leftEnd);
                int rightSignificant = skipLeadingZeros(rightName, rightIndex, rightEnd);
                int significantLengthComparison = Integer.compare(
                        leftEnd - leftSignificant, rightEnd - rightSignificant);
                if (significantLengthComparison != 0) {
                    return significantLengthComparison;
                }
                for (int offset = 0; offset < leftEnd - leftSignificant; offset++) {
                    int digitComparison = Character.compare(
                            leftName.charAt(leftSignificant + offset),
                            rightName.charAt(rightSignificant + offset));
                    if (digitComparison != 0) {
                        return digitComparison;
                    }
                }
                if (zeroPaddingComparison == 0) {
                    zeroPaddingComparison = Integer.compare(
                            leftEnd - leftIndex, rightEnd - rightIndex);
                }
                leftIndex = leftEnd;
                rightIndex = rightEnd;
                continue;
            }

            int characterComparison = Character.compare(
                    Character.toLowerCase(leftChar), Character.toLowerCase(rightChar));
            if (characterComparison != 0) {
                return characterComparison;
            }
            leftIndex++;
            rightIndex++;
        }

        int remainingLengthComparison = Integer.compare(
                leftName.length() - leftIndex, rightName.length() - rightIndex);
        if (remainingLengthComparison != 0) {
            return remainingLengthComparison;
        }
        if (zeroPaddingComparison != 0) {
            return zeroPaddingComparison;
        }
        return leftName.compareTo(rightName);
    }

    private int digitRunEnd(String value, int start) {
        int end = start;
        while (end < value.length() && isAsciiDigit(value.charAt(end))) {
            end++;
        }
        return end;
    }

    private int skipLeadingZeros(String value, int start, int end) {
        int significant = start;
        while (significant < end && value.charAt(significant) == '0') {
            significant++;
        }
        return significant;
    }

    private boolean isAsciiDigit(char value) {
        return value >= '0' && value <= '9';
    }

    @Override
    public boolean deleteMangaById(long id) {
        return mangaMapper.deleteMangaByIdAndUserId(id, UserContext.requireCurrentUserId()) > 0;
    }

    @Override
    public boolean updateFavoriteStatus(long id, boolean favorite) {
        return mangaMapper.updateFavoriteByIdAndUserId(id, favorite, UserContext.requireCurrentUserId()) > 0;
    }

    @Override
    public boolean restoreMangaById(long id) {
        return mangaMapper.restoreMangaById(id, UserContext.requireCurrentUserId()) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addTagToManga(long mangaId, Integer tagId) {
        int userId = UserContext.requireCurrentUserId();
        Manga manga = mangaMapper.getOwnedMangaById(mangaId, userId);
        MangaTag tag = mangaTagService.getOwnedTagById(tagId);
        if (manga == null || tag == null) {
            return false;
        }
        mangaTagRelationMapper.insertRelation(manga.getId(), tagId, userId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTagFromManga(long mangaId, Integer tagId) {
        int userId = UserContext.requireCurrentUserId();
        Manga manga = mangaMapper.getOwnedMangaById(mangaId, userId);
        MangaTag tag = mangaTagService.getOwnedTagById(tagId);
        if (manga == null || tag == null) {
            return false;
        }
        mangaTagRelationMapper.deleteRelation(manga.getId(), tagId, userId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean realDeleteMangaPage(int mangaId, int chapterId, int pageNum) {
        int userId = UserContext.requireCurrentUserId();
        // 查询漫画详情
        Manga manga = mangaMapper.selectOwnedMangaByIdForUpdate(Long.valueOf(mangaId), userId);
        if (manga == null) {
            return false;
        }
        String pagesJsonString = manga.getPages();
        if (pagesJsonString == null || pagesJsonString.isEmpty()) {
            return false;
        }

        // pages字段处理
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> mangaPages = objectMapper.readValue(pagesJsonString, List.class);
            String pagePath = null;
            // 检查章节是否存在
            for (Map<String, Object> chapter : mangaPages) {
                if (chapter.get("chapter").equals(chapterId)) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> pageList = (List<Map<String, Object>>) chapter.get("pagelist");
                    // 获取指定页面的路径（文件名）
                    for (Map<String, Object> page : pageList) {
                        if (page.get("page").equals(pageNum)) {
                            pagePath = (String) page.get("path");
                            break;
                        }
                    }
                    pageList.removeIf(page -> page.get("page").equals(pageNum));
                    // 更新章节的页面列表
                    chapter.put("pagelist", pageList);
                    break;
                }
            }
            if (pagePath == null) {
                return false;
            }
            // 更新漫画的页面JSON字符串
            manga.setPages(objectMapper.writeValueAsString(mangaPages));
            boolean updateResult = updateOwnedManga(manga, userId);
            if (!updateResult) {
                throw new IllegalStateException("更新漫画页面信息失败");
            }
            deleteAfterCommit(resolveMangaPath(pagePath));
            return true;
        } catch (JacksonException e) {
            throw new IllegalStateException("解析漫画页面信息失败", e);
        }
    }

    @Override
    public MangaDetailVO getRandomManga() {
        // 从数据库中随机获取一个漫画
        Manga manga = mangaMapper.getRandomRecord(UserContext.requireCurrentUserId());
        if (manga == null) {
            return null;
        }
        // 转换为VO对象
        return getMangaById(Long.valueOf(manga.getId()));
    }

    private boolean updateOwnedManga(Manga manga, int userId) {
        LambdaUpdateWrapper<Manga> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Manga::getId, manga.getId())
                .eq(Manga::getUserId, userId)
                .eq(Manga::getDeleted, false);
        return mangaMapper.update(manga, updateWrapper) > 0;
    }

    private Path resolveMangaPath(String relativePath) {
        try {
            return fileStorageService.resolveManagedPath(mangaFolder, relativePath);
        } catch (IOException e) {
            throw new IllegalStateException("漫画文件路径无效: " + relativePath, e);
        }
    }

    private void deleteAfterCommit(Path path) {
        try {
            fileStorageService.deleteAfterCommit(Collections.singletonList(path));
        } catch (IOException e) {
            throw new IllegalStateException("登记漫画文件删除失败: " + path, e);
        }
    }

}
