package com.shiyq.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统一管理上传目录中的暂存、移动和事务完成后的文件补偿。
 * 数据库提交失败时清理新文件；数据库提交成功后再执行删除，失败的删除会进入重试队列。
 */
@Slf4j
@Service
public class FileStorageService {

    private static final String STAGING_FOLDER = ".staging";
    private static final String PENDING_DELETE_FOLDER = ".pending-delete";

    @Value("${file.uploadFolder}")
    private String uploadFolder;

    public Path resolveManagedPath(String subFolder, String relativePath) throws IOException {
        if (subFolder == null || relativePath == null) {
            throw new IOException("文件路径不能为空");
        }
        Path root = uploadRoot();
        Path base = root.resolve(subFolder).normalize();
        if (!base.startsWith(root)) {
            throw new IOException("文件目录超出上传根目录: " + subFolder);
        }
        Path target = base.resolve(relativePath).normalize();
        if (!target.startsWith(base)) {
            throw new IOException("文件路径超出指定目录: " + relativePath);
        }
        return target;
    }

    public Path createStagingDirectory(String prefix) throws IOException {
        Path stagingRoot = uploadRoot().resolve(STAGING_FOLDER);
        Files.createDirectories(stagingRoot);
        return Files.createTempDirectory(stagingRoot, prefix);
    }

    public void moveIntoPlace(Path source, Path target) throws IOException {
        requireManagedPath(source);
        requireManagedPath(target);
        if (Files.exists(target)) {
            throw new IOException("目标文件已存在: " + target);
        }
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    /**
     * 注册事务回滚补偿。用于已经移动到正式目录、但数据库尚未完成提交的新文件。
     */
    public void deleteOnRollback(Collection<Path> paths) throws IOException {
        final List<Path> managedPaths = validatePaths(paths);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    for (Path path : managedPaths) {
                        deleteQuietly(path, "事务回滚后清理新文件失败");
                    }
                }
            }
        });
    }

    /**
     * 数据库提交成功后再删除文件，避免数据库仍引用已经提前删除的文件。
     */
    public void deleteAfterCommit(Collection<Path> paths) throws IOException {
        final List<Path> managedPaths = validatePaths(paths);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            for (Path path : managedPaths) {
                deleteOrQueue(path);
            }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (Path path : managedPaths) {
                    deleteOrQueue(path);
                }
            }
        });
    }

    public void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        deleteQuietly(path, "清理文件失败");
    }

    public int retryPendingDeletes() {
        Path pendingRoot = uploadRoot().resolve(PENDING_DELETE_FOLDER);
        if (!Files.isDirectory(pendingRoot)) {
            return 0;
        }
        int cleaned = 0;
        try (DirectoryStream<Path> markers = Files.newDirectoryStream(pendingRoot, "*.path")) {
            for (Path marker : markers) {
                try {
                    List<String> lines = Files.readAllLines(marker, StandardCharsets.UTF_8);
                    if (lines.isEmpty()) {
                        Files.deleteIfExists(marker);
                        continue;
                    }
                    Path target = resolveRelativeToUploadRoot(lines.get(0));
                    deleteRecursively(target);
                    Files.deleteIfExists(marker);
                    cleaned++;
                } catch (Exception e) {
                    log.warn("重试删除文件失败，marker={}", marker, e);
                }
            }
        } catch (IOException e) {
            log.warn("读取待删除文件队列失败", e);
        }
        return cleaned;
    }

    public int cleanupStaging(Date cutoff) {
        return cleanupChildren(uploadRoot().resolve(STAGING_FOLDER), Collections.<String>emptySet(), cutoff, false);
    }

    public int cleanupUnreferencedFiles(String subFolder, Collection<String> referencedNames, Date cutoff)
            throws IOException {
        Path base = resolveManagedPath(subFolder, "");
        return cleanupChildren(base, new HashSet<>(referencedNames), cutoff, false);
    }

    public int cleanupUnreferencedNumericDirectories(String subFolder, Collection<String> referencedIds, Date cutoff)
            throws IOException {
        Path base = resolveManagedPath(subFolder, "");
        return cleanupChildren(base, new HashSet<>(referencedIds), cutoff, true);
    }

    /**
     * 只扫描结构明确的“漫画ID/章节ID/图片”路径，避免触碰封面和其他业务文件。
     */
    public int cleanupUnreferencedMangaPages(String subFolder,
                                             Map<String, Set<String>> referencedPages,
                                             Date cutoff) throws IOException {
        Path mangaRoot = resolveManagedPath(subFolder, "");
        if (!Files.isDirectory(mangaRoot)) {
            return 0;
        }
        int cleaned = 0;
        for (Map.Entry<String, Set<String>> entry : referencedPages.entrySet()) {
            Path mangaDirectory = mangaRoot.resolve(entry.getKey()).normalize();
            if (!mangaDirectory.startsWith(mangaRoot) || !Files.isDirectory(mangaDirectory)) {
                continue;
            }
            try (DirectoryStream<Path> chapters = Files.newDirectoryStream(mangaDirectory)) {
                for (Path chapter : chapters) {
                    String chapterName = chapter.getFileName().toString();
                    if (!Files.isDirectory(chapter) || !chapterName.matches("\\d+")) {
                        continue;
                    }
                    try (DirectoryStream<Path> files = Files.newDirectoryStream(chapter)) {
                        for (Path file : files) {
                            if (!Files.isRegularFile(file) || !isImageFile(file) || !isOldEnough(file, cutoff)) {
                                continue;
                            }
                            String relativePath = entry.getKey() + "/" + chapterName + "/"
                                    + file.getFileName().toString();
                            if (!entry.getValue().contains(relativePath)) {
                                try {
                                    deleteRecursively(file);
                                    cleaned++;
                                } catch (IOException e) {
                                    log.warn("清理无引用漫画页面失败，path={}", file, e);
                                }
                            }
                        }
                    }
                }
            }
        }
        return cleaned;
    }

    private int cleanupChildren(Path base, Set<String> referencedNames, Date cutoff, boolean numericDirectoriesOnly) {
        if (!Files.isDirectory(base)) {
            return 0;
        }
        int cleaned = 0;
        try (DirectoryStream<Path> children = Files.newDirectoryStream(base)) {
            for (Path child : children) {
                String name = child.getFileName().toString();
                if (referencedNames.contains(name) || !isOldEnough(child, cutoff)) {
                    continue;
                }
                if (numericDirectoriesOnly) {
                    if (!Files.isDirectory(child) || !name.matches("\\d+")) {
                        continue;
                    }
                } else if (base.getFileName() == null || !STAGING_FOLDER.equals(base.getFileName().toString())) {
                    if (!Files.isRegularFile(child)) {
                        continue;
                    }
                }
                try {
                    deleteRecursively(child);
                    cleaned++;
                } catch (IOException e) {
                    log.warn("清理无引用文件失败，path={}", child, e);
                }
            }
        } catch (IOException e) {
            log.warn("扫描文件目录失败，path={}", base, e);
        }
        return cleaned;
    }

    private boolean isOldEnough(Path path, Date cutoff) {
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            return lastModified.toMillis() <= cutoff.getTime();
        } catch (IOException e) {
            log.warn("读取文件修改时间失败，path={}", path, e);
            return false;
        }
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                || name.endsWith(".gif") || name.endsWith(".bmp") || name.endsWith(".webp");
    }

    private void deleteOrQueue(Path path) {
        try {
            deleteRecursively(path);
        } catch (IOException e) {
            log.warn("提交后删除文件失败，将加入重试队列，path={}", path, e);
            queuePendingDelete(path);
        }
    }

    private void queuePendingDelete(Path path) {
        try {
            Path root = uploadRoot();
            requireManagedPath(path);
            Path pendingRoot = root.resolve(PENDING_DELETE_FOLDER);
            Files.createDirectories(pendingRoot);
            Path marker = Files.createTempFile(pendingRoot, "delete-", ".path");
            String relativePath = root.relativize(path.toAbsolutePath().normalize()).toString();
            Files.write(marker, Collections.singletonList(relativePath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("记录待删除文件失败，path={}", path, e);
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            FileUtils.deleteDirectory(path.toFile());
        } else {
            Files.deleteIfExists(path);
        }
    }

    private void deleteQuietly(Path path, String message) {
        if (path == null) {
            return;
        }
        try {
            deleteRecursively(path);
        } catch (IOException e) {
            log.warn("{}，path={}", message, path, e);
        }
    }

    private List<Path> validatePaths(Collection<Path> paths) throws IOException {
        List<Path> result = new ArrayList<>();
        if (paths == null) {
            return result;
        }
        for (Path path : paths) {
            if (path != null) {
                requireManagedPath(path);
                result.add(path.toAbsolutePath().normalize());
            }
        }
        return result;
    }

    private Path resolveRelativeToUploadRoot(String relativePath) throws IOException {
        Path root = uploadRoot();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("待删除路径超出上传根目录: " + relativePath);
        }
        return target;
    }

    private void requireManagedPath(Path path) throws IOException {
        if (path == null || !path.toAbsolutePath().normalize().startsWith(uploadRoot())) {
            throw new IOException("文件路径不在上传根目录内: " + path);
        }
    }

    private Path uploadRoot() {
        return Paths.get(uploadFolder).toAbsolutePath().normalize();
    }
}
