package com.shiyq.service.impl;

import com.shiyq.entity.DO.Game;
import com.shiyq.entity.DO.Illustration;
import com.shiyq.entity.DO.Manga;
import com.shiyq.entity.DTO.GameUploadDTO;
import com.shiyq.entity.DTO.MangaChapterUploadDTO;
import com.shiyq.entity.DTO.MangaUploadDTO;
import com.shiyq.entity.DTO.UserContext;
import com.shiyq.entity.VO.IllustrationVO;
import com.shiyq.entity.VO.MangaChapterVO;
import com.shiyq.mapper.GameMapper;
import com.shiyq.mapper.IllustrationMapper;
import com.shiyq.mapper.MangaMapper;
import com.shiyq.mapper.UserMapper;
import com.shiyq.service.FileStorageService;
import com.shiyq.service.MangaArchiveProcessor;
import com.shiyq.service.MangaTagService;
import com.shiyq.service.MediaUrlSigner;
import com.shiyq.util.ImageConverterUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FileOperationConsistencyServiceTest {

    private static final int USER_ID = 31;

    @TempDir
    Path uploadFolder;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadFolder", uploadFolder.toString());
        UserContext.add(USER_ID);
    }

    @AfterEach
    void clearUserContext() {
        UserContext.remove();
    }

    @Test
    void illustrationUploadPublishesOnlyOriginalAndReturnsDerivedThumbnailUrl() throws Exception {
        IllustrationMapper illustrationMapper = mock(IllustrationMapper.class);
        UserMapper userMapper = mock(UserMapper.class);
        AtomicReference<Illustration> savedIllustration = new AtomicReference<>();
        when(userMapper.lockById(USER_ID)).thenReturn(USER_ID);
        when(illustrationMapper.getMaxSortOrder(USER_ID)).thenReturn(0);
        when(illustrationMapper.insert(any(Illustration.class))).thenAnswer(invocation -> {
            Illustration illustration = invocation.getArgument(0);
            illustration.setId(10);
            savedIllustration.set(illustration);
            return 1;
        });
        IllustrationServiceImpl service = new IllustrationServiceImpl();
        service.setIllustrationMapper(illustrationMapper);
        service.setUserMapper(userMapper);
        service.setFileStorageService(fileStorageService);
        MediaUrlSigner mediaUrlSigner = mock(MediaUrlSigner.class);
        when(mediaUrlSigner.sign(anyString(), anyString())).thenReturn("/api/media?signed=test");
        service.setMediaUrlSigner(mediaUrlSigner);
        ReflectionTestUtils.setField(service, "illustrationFolder", "illustrations/upload");

        IllustrationVO result = service.upload(
                new MockMultipartFile("file", "image.jpg", "image/jpeg", pngBytes()));

        assertEquals(1L, regularFileCount(uploadFolder.resolve("illustrations/upload")));
        assertTrue(savedIllustration.get().getPath().endsWith(".png"));
        assertTrue(Files.isRegularFile(uploadFolder.resolve("illustrations/upload")
                .resolve(savedIllustration.get().getPath())));
        assertEquals("/api/media?signed=test", result.getOriginalUrl());
        assertEquals("/api/media?signed=test&style=small", result.getThumbnailUrl());
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void illustrationUploadRejectsFilesOverConfiguredLimitBeforeWriting() throws Exception {
        IllustrationMapper illustrationMapper = mock(IllustrationMapper.class);
        MultipartFile oversizedFile = mock(MultipartFile.class);
        when(oversizedFile.isEmpty()).thenReturn(false);
        when(oversizedFile.getSize()).thenReturn(100L * 1024 * 1024 + 1L);

        IllustrationServiceImpl service = new IllustrationServiceImpl();
        service.setIllustrationMapper(illustrationMapper);
        service.setFileStorageService(fileStorageService);
        ReflectionTestUtils.setField(service, "maxIllustrationFileSize", "100MB");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.upload(oversizedFile));

        assertTrue(exception.getMessage().contains("100MB"));
        assertFalse(Files.exists(uploadFolder.resolve(".staging")));
        verifyNoInteractions(illustrationMapper);
    }

    @Test
    void illustrationUploadRejectsInvalidImageContentAsBadRequest() throws Exception {
        IllustrationMapper illustrationMapper = mock(IllustrationMapper.class);
        IllustrationServiceImpl service = new IllustrationServiceImpl();
        service.setIllustrationMapper(illustrationMapper);
        service.setFileStorageService(fileStorageService);
        ReflectionTestUtils.setField(service, "illustrationFolder", "illustrations/upload");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.upload(new MockMultipartFile(
                        "file", "fake.png", "image/png", "not an image".getBytes("UTF-8"))));

        assertTrue(exception.getMessage().contains("有效图片"));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
        verifyNoInteractions(illustrationMapper);
    }

    @Test
    void gameDatabaseFailureLeavesNeitherFinalNorStagingDirectory() throws Exception {
        GameMapper gameMapper = mock(GameMapper.class);
        when(gameMapper.insert(any(Game.class))).thenAnswer(invocation -> {
            ((Game) invocation.getArgument(0)).setId(7);
            return 1;
        });
        when(gameMapper.updateById(any(Game.class))).thenReturn(0);

        GameServiceImpl service = new GameServiceImpl();
        service.setGameMapper(gameMapper);
        service.setFileStorageService(fileStorageService);
        ReflectionTestUtils.setField(service, "gameFolder", "games");
        GameUploadDTO request = new GameUploadDTO();
        request.setTitle("test game");
        request.setCover(new MockMultipartFile(
                "cover", "cover.png", "image/png", pngBytes()));

        assertThrows(RuntimeException.class, () -> service.addGame(request));

        assertFalse(Files.exists(uploadFolder.resolve("games/7")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void gameUploadUsesImageContentForCoverAndPreviewExtensions() throws Exception {
        GameMapper gameMapper = mock(GameMapper.class);
        AtomicReference<Game> savedGame = new AtomicReference<>();
        when(gameMapper.insert(any(Game.class))).thenAnswer(invocation -> {
            ((Game) invocation.getArgument(0)).setId(8);
            return 1;
        });
        when(gameMapper.updateById(any(Game.class))).thenAnswer(invocation -> {
            savedGame.set(invocation.getArgument(0));
            return 1;
        });

        GameServiceImpl service = new GameServiceImpl();
        service.setGameMapper(gameMapper);
        service.setFileStorageService(fileStorageService);
        ReflectionTestUtils.setField(service, "gameFolder", "games");

        GameUploadDTO request = new GameUploadDTO();
        request.setTitle("test game");
        request.setCover(new MockMultipartFile("cover", "cover.jpg", "image/jpeg", pngBytes()));
        request.setIcon(new MockMultipartFile("icon", "icon.ico", "image/x-icon", pngBytes()));
        request.setPreviewImages(new MockMultipartFile[] {
                new MockMultipartFile("preview", "preview.jpg", "image/jpeg", pngBytes())
        });

        service.addGame(request);

        assertEquals("8/cover.png", savedGame.get().getCover());
        assertEquals("8/icon.ico", savedGame.get().getIcon());
        assertEquals("8/preview_1.png", savedGame.get().getPreviewImages().get(0));
        assertTrue(Files.isRegularFile(uploadFolder.resolve("games/8/cover.png")));
        assertTrue(ImageConverterUtil.isIcoFile(uploadFolder.resolve("games/8/icon.ico").toFile()));
        assertTrue(Files.isRegularFile(uploadFolder.resolve("games/8/preview_1.png")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadPublishesOnlyTheCompletedDirectory() throws Exception {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        MangaTagService mangaTagService = mock(MangaTagService.class);
        AtomicReference<Manga> savedManga = new AtomicReference<>();
        when(mangaMapper.insert(any(Manga.class))).thenAnswer(invocation -> {
            Manga manga = invocation.getArgument(0);
            manga.setId(9);
            savedManga.set(manga);
            return 1;
        });
        when(mangaMapper.updateById(any(Manga.class))).thenReturn(1);
        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);
        service.setMangaTagService(mangaTagService);
        service.setFileStorageService(fileStorageService);
        service.setMangaArchiveProcessor(new MangaArchiveProcessor());
        service.setObjectMapper(new ObjectMapper());
        ReflectionTestUtils.setField(service, "mangaFolder", "manga");
        MangaUploadDTO request = new MangaUploadDTO();
        request.setTitle("test manga");
        request.setFile(new MockMultipartFile("file", "book.zip", "application/zip", mangaZipBytes()));

        service.addManga(request);

        assertFalse(Files.exists(uploadFolder.resolve("manga/9/cover.jpg")));
        assertTrue(Files.isRegularFile(uploadFolder.resolve("manga/9/1/1.png")));
        long storedSize = directorySize(uploadFolder.resolve("manga/9"));
        assertEquals(storedSize, savedManga.get().getSize());
        assertTrue(savedManga.get().getPages().contains("9/1/1.png"));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadAcceptsFlatAndWrappedArchiveStructures() throws Exception {
        MangaServiceImpl flatService = mangaServiceForUpload(16);
        flatService.addManga(mangaRequest(zipBytes("page.png", pngBytes())));

        MangaServiceImpl wrappedService = mangaServiceForUpload(17);
        wrappedService.addManga(mangaRequest(zipBytes(
                "outer/manga/chapter/page.png", pngBytes())));

        assertTrue(Files.isRegularFile(uploadFolder.resolve("manga/16/1/1.png")));
        assertTrue(Files.isRegularFile(uploadFolder.resolve("manga/17/1/1.png")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadRenumbersChaptersAndPagesUsingNaturalOrder() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(20);

        service.addManga(mangaRequest(naturalOrderMangaZipBytes()));

        assertImageColor("manga/20/1/1.png", 0x110000);
        assertImageColor("manga/20/1/2.png", 0x220000);
        assertImageColor("manga/20/1/3.png", 0x330000);
        assertImageColor("manga/20/2/1.png", 0x440000);
        assertImageColor("manga/20/3/1.png", 0x550000);
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadRejectsArchiveTraversalAndCleansStagingDirectory() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(10);
        MangaUploadDTO request = mangaRequest(zipBytes("../outside.png", pngBytes()));

        assertThrows(RuntimeException.class, () -> service.addManga(request));

        assertFalse(Files.exists(uploadFolder.resolve("outside.png")));
        assertFalse(Files.exists(uploadFolder.resolve("manga/10")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadRejectsTooManyArchiveEntries() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(11);
        ReflectionTestUtils.setField(archiveProcessor(service), "maxZipEntries", 1);
        MangaUploadDTO request = mangaRequest(zipBytes(
                "chapter/1.png", pngBytes(), "chapter/2.png", pngBytes()));

        assertThrows(RuntimeException.class, () -> service.addManga(request));

        assertFalse(Files.exists(uploadFolder.resolve("manga/11")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadRejectsArchiveWhoseExpandedSizeExceedsLimit() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(12);
        ReflectionTestUtils.setField(archiveProcessor(service), "maxZipExtractedSize", "10B");
        MangaUploadDTO request = mangaRequest(zipBytes("chapter/page.png", pngBytes()));

        assertThrows(RuntimeException.class, () -> service.addManga(request));

        assertFalse(Files.exists(uploadFolder.resolve("manga/12")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadRejectsArchiveEntryWhoseExpandedSizeExceedsLimit() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(18);
        ReflectionTestUtils.setField(archiveProcessor(service), "maxZipEntrySize", "10B");
        MangaUploadDTO request = mangaRequest(zipBytes("chapter/page.png", pngBytes()));

        assertThrows(RuntimeException.class, () -> service.addManga(request));

        assertFalse(Files.exists(uploadFolder.resolve("manga/18")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadIgnoresKnownArchiveMetadata() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(19);
        MangaUploadDTO request = mangaRequest(zipBytes(
                "chapter/page.png", pngBytes(), "chapter/.DS_Store", "metadata".getBytes("UTF-8")));

        service.addManga(request);

        assertTrue(Files.isRegularFile(uploadFolder.resolve("manga/19/1/1.png")));
        assertEquals(1L, regularFileCount(uploadFolder.resolve("manga/19"))
                + regularFileCount(uploadFolder.resolve("manga/19/1")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadRejectsUnsupportedFilesInChapter() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(13);
        MangaUploadDTO request = mangaRequest(zipBytes(
                "chapter/page.png", pngBytes(), "chapter/readme.html", "html".getBytes("UTF-8")));

        assertThrows(RuntimeException.class, () -> service.addManga(request));

        assertFalse(Files.exists(uploadFolder.resolve("manga/13")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadRejectsFakeImageContent() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(14);
        MangaUploadDTO request = mangaRequest(zipBytes(
                "chapter/page.png", "not an image".getBytes("UTF-8")));

        assertThrows(RuntimeException.class, () -> service.addManga(request));

        assertFalse(Files.exists(uploadFolder.resolve("manga/14")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaUploadRejectsFileWithZipExtensionButInvalidContent() throws Exception {
        MangaServiceImpl service = mangaServiceForUpload(15);
        MangaUploadDTO request = mangaRequest("not a zip".getBytes());

        assertThrows(RuntimeException.class, () -> service.addManga(request));

        assertFalse(Files.exists(uploadFolder.resolve("manga/15")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaChapterUploadAppendsTheNextChapterAndUpdatesStoredSize() throws Exception {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        Manga manga = new Manga();
        manga.setId(21);
        manga.setSize(100L);
        manga.setPages("[{\"chapter\":1,\"title\":\"第一话\",\"pagelist\":[] }]");
        when(mangaMapper.selectOwnedMangaByIdForUpdate(21L, USER_ID)).thenReturn(manga);
        AtomicReference<String> savedPages = new AtomicReference<>();
        AtomicReference<Long> savedSize = new AtomicReference<>();
        when(mangaMapper.updateChapterData(eq(21L), eq(USER_ID), anyString(), anyLong()))
                .thenAnswer(invocation -> {
                    savedPages.set(invocation.getArgument(2));
                    savedSize.set(invocation.getArgument(3));
                    return 1;
                });
        Files.createDirectories(uploadFolder.resolve("manga/21/1"));
        Files.write(uploadFolder.resolve("manga/21/1/existing-page.bin"), new byte[100]);
        MangaServiceImpl service = mangaServiceForChapterUpload(mangaMapper);
        MangaChapterUploadDTO request = mangaChapterRequest(zipBytes(
                "10.png", pngBytes(0x220000), "2.png", pngBytes(0x110000)));
        request.setTitle("  第二话  ");

        MangaChapterVO result = service.addMangaChapter(21L, request);

        assertEquals(2, result.getChapter());
        assertEquals("第二话", result.getTitle());
        assertEquals(2, result.getPageCount());
        assertImageColor("manga/21/2/1.png", 0x110000);
        assertImageColor("manga/21/2/2.png", 0x220000);
        assertTrue(savedPages.get().contains("\"chapter\":2"));
        assertTrue(savedPages.get().contains("\"title\":\"第二话\""));
        assertTrue(savedPages.get().contains("21/2/1.png"));
        assertEquals(directorySize(uploadFolder.resolve("manga/21")), savedSize.get());
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaChapterUploadRejectsAnArchiveContainingMultipleChapters() throws Exception {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        MangaServiceImpl service = mangaServiceForChapterUpload(mangaMapper);
        MangaChapterUploadDTO request = mangaChapterRequest(zipBytes(
                "first/page.png", pngBytes(), "second/page.png", pngBytes()));

        RuntimeException exception = assertThrows(
                RuntimeException.class, () -> service.addMangaChapter(22L, request));

        assertTrue(exception.getMessage().contains("只能包含一个章节"));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
        verifyNoInteractions(mangaMapper);
    }

    @Test
    void mangaChapterDatabaseFailureRemovesThePublishedChapterDirectory() throws Exception {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        Manga manga = new Manga();
        manga.setId(23);
        manga.setSize(0L);
        manga.setPages("[]");
        when(mangaMapper.selectOwnedMangaByIdForUpdate(23L, USER_ID)).thenReturn(manga);
        when(mangaMapper.updateChapterData(eq(23L), eq(USER_ID), anyString(), anyLong()))
                .thenReturn(0);
        Files.createDirectories(uploadFolder.resolve("manga/23"));
        MangaServiceImpl service = mangaServiceForChapterUpload(mangaMapper);

        assertThrows(IllegalStateException.class,
                () -> service.addMangaChapter(23L, mangaChapterRequest(mangaZipBytes())));

        assertFalse(Files.exists(uploadFolder.resolve("manga/23/1")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    @Test
    void mangaChapterUploadRejectsAMangaOutsideTheCurrentUser() throws Exception {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        when(mangaMapper.selectOwnedMangaByIdForUpdate(24L, USER_ID)).thenReturn(null);
        MangaServiceImpl service = mangaServiceForChapterUpload(mangaMapper);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.addMangaChapter(24L, mangaChapterRequest(mangaZipBytes())));

        assertTrue(exception.getMessage().contains("不存在或已删除"));
        assertFalse(Files.exists(uploadFolder.resolve("manga/24")));
        assertEquals(0L, childCount(uploadFolder.resolve(".staging")));
    }

    private byte[] pngBytes() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private byte[] mangaZipBytes() throws Exception {
        return zipBytes("chapter/page.jpg", pngBytes());
    }

    private byte[] naturalOrderMangaZipBytes() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            addZipEntry(zip, "chapter10/1.png", pngBytes(0x550000));
            addZipEntry(zip, "chapter2/1.png", pngBytes(0x440000));
            addZipEntry(zip, "chapter1/10.png", pngBytes(0x330000));
            addZipEntry(zip, "chapter1/2.png", pngBytes(0x220000));
            addZipEntry(zip, "chapter1/1.png", pngBytes(0x110000));
        }
        return output.toByteArray();
    }

    private byte[] pngBytes(int rgb) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setRGB(x, y, rgb);
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private void addZipEntry(ZipOutputStream zip, String name, byte[] content) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }

    private void assertImageColor(String relativePath, int expectedRgb) throws Exception {
        BufferedImage image = ImageIO.read(uploadFolder.resolve(relativePath).toFile());
        assertEquals(expectedRgb, image.getRGB(0, 0) & 0xFFFFFF);
    }

    private byte[] zipBytes(String name, byte[] content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content);
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private byte[] zipBytes(String firstName, byte[] firstContent,
                            String secondName, byte[] secondContent) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(firstName));
            zip.write(firstContent);
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(secondName));
            zip.write(secondContent);
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private MangaServiceImpl mangaServiceForUpload(int mangaId) {
        MangaMapper mangaMapper = mock(MangaMapper.class);
        MangaTagService mangaTagService = mock(MangaTagService.class);
        when(mangaMapper.insert(any(Manga.class))).thenAnswer(invocation -> {
            ((Manga) invocation.getArgument(0)).setId(mangaId);
            return 1;
        });
        when(mangaMapper.updateById(any(Manga.class))).thenReturn(1);
        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);
        service.setMangaTagService(mangaTagService);
        service.setFileStorageService(fileStorageService);
        service.setMangaArchiveProcessor(new MangaArchiveProcessor());
        service.setObjectMapper(new ObjectMapper());
        ReflectionTestUtils.setField(service, "mangaFolder", "manga");
        return service;
    }

    private MangaServiceImpl mangaServiceForChapterUpload(MangaMapper mangaMapper) {
        MangaServiceImpl service = new MangaServiceImpl();
        service.setMangaMapper(mangaMapper);
        service.setFileStorageService(fileStorageService);
        service.setMangaArchiveProcessor(new MangaArchiveProcessor());
        service.setObjectMapper(new ObjectMapper());
        ReflectionTestUtils.setField(service, "mangaFolder", "manga");
        return service;
    }

    private MangaArchiveProcessor archiveProcessor(MangaServiceImpl service) {
        return (MangaArchiveProcessor) ReflectionTestUtils.getField(
                service, "mangaArchiveProcessor");
    }

    private MangaUploadDTO mangaRequest(byte[] archive) {
        MangaUploadDTO request = new MangaUploadDTO();
        request.setTitle("test manga");
        request.setFile(new MockMultipartFile("file", "book.zip", "application/zip", archive));
        return request;
    }

    private MangaChapterUploadDTO mangaChapterRequest(byte[] archive) {
        MangaChapterUploadDTO request = new MangaChapterUploadDTO();
        request.setTitle("new chapter");
        request.setFile(new MockMultipartFile(
                "file", "chapter.zip", "application/zip", archive));
        return request;
    }

    private long directorySize(Path directory) throws Exception {
        try (java.util.stream.Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).sum();
        }
    }

    private long regularFileCount(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            return 0L;
        }
        try (java.util.stream.Stream<Path> files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).count();
        }
    }

    private long childCount(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            return 0L;
        }
        try (java.util.stream.Stream<Path> files = Files.list(directory)) {
            return files.count();
        }
    }
}
