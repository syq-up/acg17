package com.shiyq.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileStorageServiceTest {

    @TempDir
    Path uploadFolder;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService();
        ReflectionTestUtils.setField(service, "uploadFolder", uploadFolder.toString());
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deleteWaitsUntilTheDatabaseTransactionCommits() throws Exception {
        Path file = createFile("manga/1/page.webp");
        TransactionSynchronizationManager.initSynchronization();

        service.deleteAfterCommit(Collections.singletonList(file));
        assertTrue(Files.exists(file));

        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
            synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        }
        assertFalse(Files.exists(file));
    }

    @Test
    void rollbackRemovesFilesMovedOutOfStaging() throws Exception {
        Path staging = service.createStagingDirectory("test-");
        Path stagedFile = Files.write(staging.resolve("image.webp"), Collections.singletonList("x"));
        Path finalFile = service.resolveManagedPath("illustrations/upload", "image.webp");
        service.moveIntoPlace(stagedFile, finalFile);
        TransactionSynchronizationManager.initSynchronization();

        service.deleteOnRollback(Collections.singletonList(finalFile));
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        assertFalse(Files.exists(finalFile));
    }

    @Test
    void pathsCannotEscapeTheManagedFolder() {
        assertThrows(IOException.class,
                () -> service.resolveManagedPath("illustrations/upload", "../../outside.webp"));
    }

    @Test
    void derivedCacheFilesCannotBeReadAsSignedSourceMedia() throws Exception {
        createFile("media-cache/derived.webp");

        assertThrows(IOException.class,
                () -> service.resolveReadableFile("media-cache/derived.webp"));
    }

    @Test
    void maintenanceKeepsReferencedFilesAndRemovesOldOrphans() throws Exception {
        Path referenced = createFile("illustrations/upload/kept.webp");
        Path orphan = createFile("illustrations/upload/orphan.webp");
        Path referencedManga = createFile("manga/1/page.webp").getParent();
        Path orphanManga = createFile("manga/2/page.webp").getParent();
        FileTime oldTime = FileTime.fromMillis(1_600_000_000_000L);
        Files.setLastModifiedTime(referenced, oldTime);
        Files.setLastModifiedTime(orphan, oldTime);
        Files.setLastModifiedTime(referencedManga, oldTime);
        Files.setLastModifiedTime(orphanManga, oldTime);
        Date cutoff = new Date(1_700_000_000_000L);

        int fileCount = service.cleanupUnreferencedFiles(
                "illustrations/upload", Collections.singletonList("kept.webp"), cutoff);
        int directoryCount = service.cleanupUnreferencedNumericDirectories(
                "manga", Arrays.asList("1"), cutoff);

        assertEquals(1, fileCount);
        assertEquals(1, directoryCount);
        assertTrue(Files.exists(referenced));
        assertFalse(Files.exists(orphan));
        assertTrue(Files.exists(referencedManga));
        assertFalse(Files.exists(orphanManga));
    }

    @Test
    void maintenanceOnlyRemovesUnreferencedMangaPageImages() throws Exception {
        Path referenced = createFile("manga/1/1/kept.webp");
        Path orphan = createFile("manga/1/1/orphan.webp");
        Path rootMetadata = createFile("manga/1/metadata.txt");
        FileTime oldTime = FileTime.fromMillis(1_600_000_000_000L);
        Files.setLastModifiedTime(referenced, oldTime);
        Files.setLastModifiedTime(orphan, oldTime);
        Files.setLastModifiedTime(rootMetadata, oldTime);
        Map<String, Set<String>> references = new HashMap<>();
        references.put("1", new HashSet<>(Collections.singletonList("1/1/kept.webp")));

        int cleaned = service.cleanupUnreferencedMangaPages(
                "manga", references, new Date(1_700_000_000_000L));

        assertEquals(1, cleaned);
        assertTrue(Files.exists(referenced));
        assertFalse(Files.exists(orphan));
        assertTrue(Files.exists(rootMetadata));
    }

    private Path createFile(String relativePath) throws IOException {
        Path path = uploadFolder.resolve(relativePath);
        Files.createDirectories(path.getParent());
        return Files.write(path, Collections.singletonList("x"));
    }
}
