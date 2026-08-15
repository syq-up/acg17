package com.shiyq.service.impl;

import com.shiyq.entity.DO.Manga;
import com.shiyq.entity.DO.MangaTag;
import com.shiyq.entity.DTO.MangaChapterData;
import com.shiyq.entity.DTO.MangaChapterUploadDTO;
import com.shiyq.entity.DTO.MangaPageData;
import com.shiyq.entity.DTO.MangaUpdateDTO;
import com.shiyq.entity.DTO.MangaUploadDTO;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.MangaChapterVO;
import com.shiyq.entity.VO.MangaTagVO;
import com.shiyq.entity.VO.MangaDetailVO;
import com.shiyq.mapper.MangaMapper;
import com.shiyq.mapper.MangaTagRelationMapper;
import com.shiyq.service.FileStorageService;
import com.shiyq.service.MangaArchiveProcessor;
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
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.commons.io.FileUtils;

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

    private static final int MAX_TAGS = 100;

    private MangaMapper mangaMapper;
    private MangaTagRelationMapper mangaTagRelationMapper;
    private MangaTagService mangaTagService;
    private FileStorageService fileStorageService;
    private MangaArchiveProcessor mangaArchiveProcessor;
    private MediaUrlSigner mediaUrlSigner;
    private ObjectMapper objectMapper;
    
    @Value("${file.mangaFolder}")
    private String mangaFolder;
    
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
    public void setMangaArchiveProcessor(MangaArchiveProcessor mangaArchiveProcessor) {
        this.mangaArchiveProcessor = mangaArchiveProcessor;
    }

    @Autowired
    public void setMediaUrlSigner(MediaUrlSigner mediaUrlSigner) {
        this.mediaUrlSigner = mediaUrlSigner;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public PageVO<MangaVO> getList(long pageNum, boolean deleted, String title,
                                  List<Integer> tagIds) {
        if (pageNum <= 0) {
            throw new IllegalArgumentException("页码必须大于0");
        }
        if (tagIds != null && tagIds.stream().anyMatch(tagId -> tagId == null || tagId <= 0)) {
            throw new IllegalArgumentException("标签ID必须大于0");
        }
        List<Integer> normalizedTagIds = tagIds == null
                ? Collections.emptyList()
                : new ArrayList<>(new LinkedHashSet<>(tagIds));
        title = normalizeOptionalText(title, "标题", 255);
        int userId = UserContext.requireCurrentUserId();
        // 默认页大小为 30
        PageVO<MangaVO> pageVO = new PageVO<>(30L, pageNum);
        // 查询漫画作品列表
        List<Manga> list = mangaMapper.getListByCondition(userId, pageNum, 30L, deleted,
                title, normalizedTagIds);
        
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
                title, normalizedTagIds));
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
        
        List<MangaChapterData> chapters = parseChapterData(manga.getPages());
        addPageAccessUrls(chapters);
        mangaDetailVO.setPages(chapters);
        
        populateTagGroups(mangaDetailVO, mangaTagService.getTagsByMangaId(manga.getId()));
        
        return mangaDetailVO;
    }

    private void addPageAccessUrls(List<MangaChapterData> chapters) {
        for (MangaChapterData chapter : chapters) {
            for (MangaPageData page : chapter.getPagelist()) {
                page.setPath(generateAccessUrl(page.getPath()));
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
        detail.setArtistTags(new ArrayList<>());
        detail.setGroupTags(new ArrayList<>());
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
            } else if (MangaConstant.TAG_CATEGORY_ARTIST.equals(tag.getCategory())) {
                detail.getArtistTags().add(tag);
            } else if (MangaConstant.TAG_CATEGORY_GROUP.equals(tag.getCategory())) {
                detail.getGroupTags().add(tag);
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
            Set<Integer> tagIds = resolveTagIds(tagsJson);
            
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
                    Path stagedMangaDirectory = stagingDirectory.resolve(String.valueOf(manga.getId()));
                    mangaArchiveProcessor.extractManga(tempZipFile, stagedMangaDirectory.toFile());
                    // 设置漫画封面路径和大小
                    manga.setCover(manga.getId() + "/cover.jpg");
                    long storedSize = FileUtils.sizeOfDirectory(stagedMangaDirectory.toFile());
                    manga.setSize(storedSize);
                    
                    // 生成pages参数
                    String pages = generatePagesJson(stagedMangaDirectory.toFile(), manga.getId());
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
            mangaArchiveProcessor.extractChapter(tempZipFile, stagedChapterDirectory.toFile());

            int userId = UserContext.requireCurrentUserId();
            Manga manga = mangaMapper.selectOwnedMangaByIdForUpdate(mangaId, userId);
            if (manga == null) {
                throw new IllegalArgumentException("漫画不存在或已删除");
            }

            List<MangaChapterData> chapters = parseChapterData(manga.getPages());
            int chapterNumber = nextChapterNumber(chapters);
            List<MangaPageData> pageList = buildChapterPageList(
                    stagedChapterDirectory.toFile(), manga.getId(), chapterNumber);

            chapters.add(new MangaChapterData(chapterNumber, chapterTitle, pageList));

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

        LambdaUpdateWrapper<Manga> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Manga::getId, id)
                .eq(Manga::getUserId, UserContext.requireCurrentUserId())
                .eq(Manga::getDeleted, false);
        return mangaMapper.update(manga, updateWrapper) > 0;
    }
    
    private Set<Integer> resolveTagIds(String tagsJson) {
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

    private List<MangaChapterData> parseChapterData(String pages) {
        if (pages == null || pages.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<MangaChapterData> chapters = objectMapper.readValue(
                pages, new TypeReference<List<MangaChapterData>>() { });
        if (chapters == null) {
            return new ArrayList<>();
        }
        for (MangaChapterData chapter : chapters) {
            if (chapter == null) {
                throw new IllegalStateException("漫画章节数据不能为空");
            }
            if (chapter.getPagelist() == null) {
                chapter.setPagelist(new ArrayList<>());
            }
        }
        return new ArrayList<>(chapters);
    }

    private int nextChapterNumber(List<MangaChapterData> chapters) throws IOException {
        int maximum = 0;
        Set<Integer> chapterNumbers = new HashSet<>();
        for (MangaChapterData chapter : chapters) {
            int chapterNumber = chapter.getChapter();
            if (chapterNumber <= 0) {
                throw new IOException("现有漫画章节编号无效");
            }
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

    private List<MangaPageData> buildChapterPageList(File chapterDirectory,
                                                     Integer mangaId,
                                                     int chapterNumber) throws IOException {
        File[] imageFiles = requireFiles(chapterDirectory);
        Arrays.sort(imageFiles, (left, right) -> Integer.compare(
                pageNumberFromFilename(left), pageNumberFromFilename(right)));

        List<MangaPageData> pages = new ArrayList<>();
        int pageNumber = 1;
        for (File imageFile : imageFiles) {
            pages.add(new MangaPageData(pageNumber,
                    mangaId + "/" + chapterNumber + "/" + imageFile.getName()));
            pageNumber++;
        }
        return pages;
    }

    /**
     * 生成pages参数的JSON字符串
     * @param mangaDir 漫画目录
     * @param mangaId 漫画ID
     * @return pages的JSON字符串
     */
    private String generatePagesJson(File mangaDir, Integer mangaId) throws IOException {
        try {
            File[] chapterDirectories = requireDirectories(mangaDir);
            Arrays.sort(chapterDirectories, (left, right) -> Integer.compare(
                    Integer.parseInt(left.getName()), Integer.parseInt(right.getName())));

            List<MangaChapterData> chapters = new ArrayList<>();
            for (File chapterDirectory : chapterDirectories) {
                int chapterNumber = Integer.parseInt(chapterDirectory.getName());
                List<MangaPageData> pages = buildChapterPageList(
                        chapterDirectory, mangaId, chapterNumber);
                chapters.add(new MangaChapterData(
                        chapterNumber, "第" + chapterNumber + "话", pages));
            }
            return objectMapper.writeValueAsString(chapters);
        } catch (Exception e) {
            throw new IOException("生成漫画页面数据失败", e);
        }
    }

    private File[] requireDirectories(File directory) throws IOException {
        File[] directories = directory.listFiles(File::isDirectory);
        if (directories == null || directories.length == 0) {
            throw new IOException("漫画目录中没有章节: " + directory.getName());
        }
        return directories;
    }

    private File[] requireFiles(File directory) throws IOException {
        File[] files = directory.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            throw new IOException("章节目录中没有页面: " + directory.getName());
        }
        return files;
    }

    private int pageNumberFromFilename(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        return Integer.parseInt(dotIndex < 0 ? name : name.substring(0, dotIndex));
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

        try {
            List<MangaChapterData> chapters = parseChapterData(pagesJsonString);
            String pagePath = null;
            for (MangaChapterData chapter : chapters) {
                if (chapter.getChapter() == chapterId) {
                    for (MangaPageData page : chapter.getPagelist()) {
                        if (page.getPage() == pageNum) {
                            pagePath = page.getPath();
                            break;
                        }
                    }
                    chapter.getPagelist().removeIf(page -> page.getPage() == pageNum);
                    break;
                }
            }
            if (pagePath == null) {
                return false;
            }
            manga.setPages(objectMapper.writeValueAsString(chapters));
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
