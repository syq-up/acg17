package com.shiyq.service;

import com.shiyq.entity.DO.Illustration;
import com.shiyq.entity.DO.Manga;
import com.shiyq.mapper.GameMapper;
import com.shiyq.mapper.IllustrationMapper;
import com.shiyq.mapper.MangaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecycleCleanupServiceTest {

    private static final Date CUTOFF = new Date(1_700_000_000_000L);

    @TempDir
    Path uploadFolder;

    private IllustrationMapper illustrationMapper;
    private MangaMapper mangaMapper;
    private GameMapper gameMapper;
    private RecycleCleanupService service;

    @BeforeEach
    void setUp() {
        illustrationMapper = mock(IllustrationMapper.class);
        mangaMapper = mock(MangaMapper.class);
        gameMapper = mock(GameMapper.class);

        service = new RecycleCleanupService();
        service.setIllustrationMapper(illustrationMapper);
        service.setMangaMapper(mangaMapper);
        service.setGameMapper(gameMapper);
        FileStorageService fileStorageService = new FileStorageService();
        ReflectionTestUtils.setField(fileStorageService, "uploadFolder", uploadFolder.toString());
        service.setFileStorageService(fileStorageService);
        ReflectionTestUtils.setField(service, "illustrationFolder", "illustrations/upload");
        ReflectionTestUtils.setField(service, "mangaFolder", "manga");
        ReflectionTestUtils.setField(service, "gameFolder", "games");
    }

    @Test
    void illustrationCleanupUsesTheRecordOwnerAndDeletesOriginalFile() throws Exception {
        Path original = createFile("illustrations/upload/example.webp");
        Illustration illustration = new Illustration();
        illustration.setId(8);
        illustration.setPath("example.webp");
        illustration.setSize(123);
        illustration.setUserId(42);
        when(illustrationMapper.selectExpiredByIdForUpdate(8, CUTOFF)).thenReturn(illustration);
        when(illustrationMapper.realDeleteExpiredById(8, CUTOFF)).thenReturn(1);

        assertTrue(service.cleanIllustration(8, CUTOFF));

        assertFalse(Files.exists(original));
    }

    @Test
    void restoredIllustrationIsSkippedAfterTheLockedRecheck() throws Exception {
        when(illustrationMapper.selectExpiredByIdForUpdate(8, CUTOFF)).thenReturn(null);

        assertFalse(service.cleanIllustration(8, CUTOFF));

        verify(illustrationMapper, never()).realDeleteExpiredById(8, CUTOFF);
    }

    @Test
    void databaseFailureLeavesIllustrationFilesUntouched() throws Exception {
        Path original = createFile("illustrations/upload/example.webp");
        Illustration illustration = new Illustration();
        illustration.setPath("example.webp");
        illustration.setUserId(42);
        when(illustrationMapper.selectExpiredByIdForUpdate(8, CUTOFF)).thenReturn(illustration);

        assertThrows(IllegalStateException.class, () -> service.cleanIllustration(8, CUTOFF));

        assertTrue(Files.exists(original));
        verify(illustrationMapper).realDeleteExpiredById(8, CUTOFF);
    }

    @Test
    void mangaCleanupUsesTheRecordOwner() throws Exception {
        Path mangaDirectory = uploadFolder.resolve("manga/9");
        Files.createDirectories(mangaDirectory);
        Files.write(mangaDirectory.resolve("page.webp"), Collections.singletonList("x"));
        Manga manga = new Manga();
        manga.setId(9);
        manga.setUserId(77);
        manga.setSize(456L);
        when(mangaMapper.selectExpiredByIdForUpdate(9, CUTOFF)).thenReturn(manga);
        when(mangaMapper.realDeleteExpiredById(9, CUTOFF)).thenReturn(1);

        assertTrue(service.cleanManga(9, CUTOFF));

        assertFalse(Files.exists(mangaDirectory));
    }

    private Path createFile(String relativePath) throws IOException {
        Path path = uploadFolder.resolve(relativePath);
        Files.createDirectories(path.getParent());
        return Files.write(path, Collections.singletonList("x"));
    }
}
