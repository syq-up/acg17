package com.shiyq.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.shiyq.entity.DO.Illustration;
import com.shiyq.entity.DO.Manga;
import com.shiyq.mapper.GameMapper;
import com.shiyq.mapper.IllustrationMapper;
import com.shiyq.mapper.MangaMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 回收站物理清理。每个清理方法由定时器单独调用，因此每条记录拥有独立事务。
 */
@Slf4j
@Service
public class RecycleCleanupService {

    @Value("${file.illustrationFolder}")
    private String illustrationFolder;
    @Value("${file.illustrationThumbFolder}")
    private String illustrationThumbFolder;
    @Value("${file.mangaFolder}")
    private String mangaFolder;
    @Value("${file.gameFolder}")
    private String gameFolder;

    private IllustrationMapper illustrationMapper;
    private MangaMapper mangaMapper;
    private GameMapper gameMapper;
    private FileStorageService fileStorageService;

    @Autowired
    public void setIllustrationMapper(IllustrationMapper illustrationMapper) {
        this.illustrationMapper = illustrationMapper;
    }

    @Autowired
    public void setMangaMapper(MangaMapper mangaMapper) {
        this.mangaMapper = mangaMapper;
    }

    @Autowired
    public void setGameMapper(GameMapper gameMapper) {
        this.gameMapper = gameMapper;
    }

    @Autowired
    public void setFileStorageService(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    public List<Integer> getExpiredIllustrationIds(Date cutoff) {
        return illustrationMapper.getExpiredIds(cutoff);
    }

    public List<Integer> getExpiredMangaIds(Date cutoff) {
        return mangaMapper.getExpiredIds(cutoff);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public boolean cleanIllustration(int id, Date cutoff) throws IOException {
        Illustration illustration = illustrationMapper.selectExpiredByIdForUpdate(id, cutoff);
        if (illustration == null) {
            return false;
        }

        Path originalPath = fileStorageService.resolveManagedPath(illustrationFolder, illustration.getPath());
        Path thumbPath = fileStorageService.resolveManagedPath(illustrationThumbFolder, illustration.getPath());

        if (illustrationMapper.realDeleteExpiredById(id, cutoff) != 1) {
            throw new IllegalStateException("物理删除插画记录失败: " + id);
        }
        fileStorageService.deleteAfterCommit(Arrays.asList(originalPath, thumbPath));
        return true;
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public boolean cleanManga(int id, Date cutoff) throws IOException {
        Manga manga = mangaMapper.selectExpiredByIdForUpdate(id, cutoff);
        if (manga == null) {
            return false;
        }

        Path mangaDirectory = fileStorageService.resolveManagedPath(mangaFolder, String.valueOf(manga.getId()));

        if (mangaMapper.realDeleteExpiredById(id, cutoff) != 1) {
            throw new IllegalStateException("物理删除漫画记录失败: " + id);
        }
        fileStorageService.deleteAfterCommit(Arrays.asList(mangaDirectory));
        return true;
    }

    /**
     * 清理上传失败留下的暂存文件、提交后删除失败的文件，以及超过安全期限的孤儿文件。
     */
    public int cleanupFileResidues(Date cutoff) throws IOException {
        int cleaned = fileStorageService.retryPendingDeletes();
        cleaned += fileStorageService.cleanupStaging(cutoff);

        List<String> illustrationPaths = illustrationMapper.getAll().stream()
                .map(Illustration::getPath)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        cleaned += fileStorageService.cleanupUnreferencedFiles(illustrationFolder, illustrationPaths, cutoff);
        cleaned += fileStorageService.cleanupUnreferencedFiles(illustrationThumbFolder, illustrationPaths, cutoff);

        List<Manga> mangas = mangaMapper.getAllForFileCleanup();
        List<String> mangaIds = mangas.stream()
                .map(Manga::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());
        cleaned += fileStorageService.cleanupUnreferencedNumericDirectories(mangaFolder, mangaIds, cutoff);
        cleaned += fileStorageService.cleanupUnreferencedMangaPages(
                mangaFolder, collectMangaPageReferences(mangas), cutoff);

        List<String> gameIds = gameMapper.getAllIdsForFileCleanup().stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.toList());
        cleaned += fileStorageService.cleanupUnreferencedNumericDirectories(gameFolder, gameIds, cutoff);
        return cleaned;
    }

    private Map<String, Set<String>> collectMangaPageReferences(List<Manga> mangas) {
        Map<String, Set<String>> result = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (Manga manga : mangas) {
            if (manga.getId() == null || manga.getPages() == null) {
                continue;
            }
            try {
                List<Map<String, Object>> chapters = objectMapper.readValue(
                        manga.getPages(), new TypeReference<List<Map<String, Object>>>() { });
                Set<String> paths = new HashSet<>();
                for (Map<String, Object> chapter : chapters) {
                    Object pageListValue = chapter.get("pagelist");
                    if (!(pageListValue instanceof List)) {
                        continue;
                    }
                    for (Object pageValue : (List<?>) pageListValue) {
                        if (pageValue instanceof Map) {
                            Object path = ((Map<?, ?>) pageValue).get("path");
                            if (path instanceof String) {
                                paths.add(((String) path).replace('\\', '/'));
                            }
                        }
                    }
                }
                result.put(String.valueOf(manga.getId()), paths);
            } catch (JacksonException e) {
                log.warn("漫画页面数据无法解析，跳过对应目录的页面孤儿清理，id={}", manga.getId(), e);
            }
        }
        return result;
    }

}
