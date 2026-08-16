package com.shiyq.service;

import com.shiyq.util.ImageFileInspector;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 校验并展开漫画 ZIP，将不同来源的目录和图片名称规范化为统一结构。
 */
@Component
public class MangaArchiveProcessor {

    private static final int MAX_ARCHIVE_PATH_DEPTH = 4;
    private static final int MAX_ARCHIVE_PATH_LENGTH = 1024;
    private static final int MAX_ARCHIVE_NAME_LENGTH = 255;

    @Value("${file.mangaZip.maxEntries:5000}")
    private int maxZipEntries = 5000;

    @Value("${file.mangaZip.maxEntrySize:100MB}")
    private String maxZipEntrySize = "100MB";

    @Value("${file.mangaZip.maxExtractedSize:1GB}")
    private String maxZipExtractedSize = "1GB";

    /**
     * 展开完整漫画。输出目录结构为“章节编号/页码.扩展名”。
     */
    public void extractManga(File zipFile, File targetMangaDirectory) throws IOException {
        File extractedDirectory = extractToTemporaryDirectory(zipFile, targetMangaDirectory.getParentFile());
        try {
            File mangaDirectory = analyzeAndAdjustStructure(extractedDirectory);
            copyDirectoryWithRenumbering(mangaDirectory, targetMangaDirectory);
        } catch (Exception e) {
            if (targetMangaDirectory.exists()) {
                FileUtils.deleteDirectory(targetMangaDirectory);
            }
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("文件写入失败：" + e.getMessage(), e);
        } finally {
            FileUtils.deleteDirectory(extractedDirectory);
        }
    }

    /**
     * 展开单个章节。压缩包可直接包含图片，也可包含最多两层外包目录。
     */
    public void extractChapter(File zipFile, File targetChapterDirectory) throws IOException {
        File extractedDirectory = extractToTemporaryDirectory(zipFile, targetChapterDirectory.getParentFile());
        try {
            File processedDirectory = analyzeAndAdjustStructure(extractedDirectory);
            File[] chapterDirectories = requireDirectoryItems(processedDirectory);
            if (!containsOnlyChapterDirectories(chapterDirectories)
                    || chapterDirectories.length != 1) {
                throw new IOException("一个章节压缩包只能包含一个章节");
            }
            copyChapterImages(chapterDirectories[0], targetChapterDirectory);
        } catch (IOException e) {
            if (targetChapterDirectory.exists()) {
                FileUtils.deleteDirectory(targetChapterDirectory);
            }
            throw e;
        } finally {
            FileUtils.deleteDirectory(extractedDirectory);
        }
    }

    private File extractToTemporaryDirectory(File zipFile, File stagingDirectory) throws IOException {
        validateZipSignature(zipFile);
        Files.createDirectories(stagingDirectory.toPath());
        File extractedDirectory = Files.createTempDirectory(
                stagingDirectory.toPath(), "extract-").toFile();
        try {
            extractZipFile(zipFile, extractedDirectory);
            removeIgnoredArchiveMetadata(extractedDirectory);
            return extractedDirectory;
        } catch (IOException e) {
            FileUtils.deleteDirectory(extractedDirectory);
            throw new IOException("文件处理失败：" + e.getMessage(), e);
        }
    }

    private void extractZipFile(File zipFile, File destinationDirectory) throws IOException {
        if (maxZipEntries <= 0) {
            throw new IOException("ZIP最大条目数配置必须大于0");
        }
        long maxEntryBytes = parsePositiveDataSize(maxZipEntrySize, "ZIP单文件解压上限");
        long maxExtractedBytes = parsePositiveDataSize(maxZipExtractedSize, "ZIP累计解压上限");
        Path destinationRoot = destinationDirectory.toPath().toAbsolutePath().normalize();
        Set<Path> extractedPaths = new HashSet<>();
        int entryCount = 0;
        int fileCount = 0;
        long totalExtractedBytes = 0L;

        try (ZipInputStream input = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
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
                        int length;
                        while ((length = input.read(buffer)) != -1) {
                            if (length == 0) {
                                continue;
                            }
                            if (entryExtractedBytes > maxEntryBytes - length) {
                                throw new IOException("压缩包内单个文件解压后过大: " + entry.getName());
                            }
                            if (totalExtractedBytes > maxExtractedBytes - length) {
                                throw new IOException("压缩包累计解压大小超过限制");
                            }
                            output.write(buffer, 0, length);
                            entryExtractedBytes += length;
                            totalExtractedBytes += length;
                        }
                    }
                    fileCount++;
                }
                input.closeEntry();
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
     * 接受图片、章节目录以及最多两层外包目录，返回结构统一的漫画目录。
     */
    private File analyzeAndAdjustStructure(File extractedDirectory) throws IOException {
        File candidate = extractedDirectory;
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

    private void copyDirectoryWithRenumbering(File sourceDirectory,
                                               File targetDirectory) throws IOException {
        Files.createDirectories(targetDirectory.toPath());
        File[] chapterDirectories = requireDirectoryItems(sourceDirectory);
        if (!containsOnlyChapterDirectories(chapterDirectories)) {
            throw new IOException("漫画目录只能包含章节文件夹和图片");
        }
        Arrays.sort(chapterDirectories, this::compareNaturalNames);

        int chapterNumber = 1;
        for (File chapterDirectory : chapterDirectories) {
            copyChapterImages(chapterDirectory,
                    new File(targetDirectory, String.valueOf(chapterNumber)));
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
            String extension = ImageFileInspector.inspect(imageFile).extension();
            File targetFile = new File(targetChapterDirectory, pageNumber + "." + extension);
            FileUtils.copyFile(imageFile, targetFile);
            pageNumber++;
        }
    }

    private boolean isImageFile(File file) {
        try {
            ImageFileInspector.inspect(file);
            return true;
        } catch (IOException e) {
            return false;
        }
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
                int lengthComparison = Integer.compare(
                        leftEnd - leftSignificant, rightEnd - rightSignificant);
                if (lengthComparison != 0) {
                    return lengthComparison;
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
}
