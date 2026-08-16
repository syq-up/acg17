package com.shiyq.controller;

import com.shiyq.service.FileStorageService;
import com.shiyq.service.MediaImageProcessor;
import com.shiyq.service.MediaStyle;
import com.shiyq.service.MediaUrlSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MediaControllerTest {

    private static final String SECRET = "test-media-secret-with-at-least-32-bytes";

    @TempDir
    Path uploadFolder;

    private MediaUrlSigner signer;
    private MediaController controller;

    @BeforeEach
    void setUp() {
        signer = new MediaUrlSigner(
                SECRET,
                10,
                "/api/media",
                Clock.fixed(Instant.parse("2026-08-13T12:00:00Z"), ZoneOffset.UTC));
        FileStorageService storageService = new FileStorageService();
        ReflectionTestUtils.setField(storageService, "uploadFolder", uploadFolder.toString());
        controller = new MediaController(signer, storageService, mock(MediaImageProcessor.class));
    }

    @Test
    void validSignedUrlReturnsOnlyTheSignedFile() throws Exception {
        byte[] content = "signed-media".getBytes(StandardCharsets.UTF_8);
        Path file = uploadFolder.resolve("manga/9/1/page.png");
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        Map<String, String> query = queryOf(signer.sign("manga/9/1/page.png"));

        ResponseEntity<Resource> response = controller.getMedia(
                query.get("path"), Long.parseLong(query.get("expires")), query.get("signature"), null);

        assertEquals(200, response.getStatusCode().value());
        assertArrayEquals(content, response.getBody().getInputStream().readAllBytes());
    }

    @Test
    void tamperedPathIsForbiddenEvenWhenTheTargetExists() throws Exception {
        Path otherFile = uploadFolder.resolve("manga/10/1/page.png");
        Files.createDirectories(otherFile.getParent());
        Files.write(otherFile, new byte[] { 1 });
        Map<String, String> signed = queryOf(signer.sign("manga/9/1/page.png"));
        Map<String, String> other = queryOf(signer.sign("manga/10/1/page.png"));

        ResponseEntity<Resource> response = controller.getMedia(
                other.get("path"), Long.parseLong(signed.get("expires")), signed.get("signature"), null);

        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void unknownStyleIsRejectedBeforeReadingTheMediaFile() {
        ResponseEntity<Resource> response = controller.getMedia(
                "not-a-signed-path", signer.remainingSeconds(0), "invalid", "SMALL");

        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void styledResponseUsesTheProcessorAndWebpFilename() throws Exception {
        byte[] original = "source".getBytes(StandardCharsets.UTF_8);
        Path file = uploadFolder.resolve("manga/9/1/page.png");
        Files.createDirectories(file.getParent());
        Files.write(file, original);
        Map<String, String> query = queryOf(signer.sign("manga/9/1/page.png"));

        Path derived = uploadFolder.resolve("media-cache/result.webp");
        Files.createDirectories(derived.getParent());
        byte[] webp = "derived".getBytes(StandardCharsets.UTF_8);
        Files.write(derived, webp);
        MediaImageProcessor processor = mock(MediaImageProcessor.class);
        when(processor.process(any(Path.class), eq("manga/9/1/page.png"), eq(MediaStyle.SMALL)))
                .thenReturn(new MediaImageProcessor.MediaVariant(derived, MediaType.parseMediaType("image/webp")));
        MediaController styledController = new MediaController(signer,
                storageService(), processor);

        ResponseEntity<Resource> response = styledController.getMedia(
                query.get("path"), Long.parseLong(query.get("expires")), query.get("signature"), "small");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.parseMediaType("image/webp"), response.getHeaders().getContentType());
        assertArrayEquals(webp, response.getBody().getInputStream().readAllBytes());
        assertTrue(response.getHeaders().getFirst("Content-Disposition").contains("page.webp"));
        verify(processor).process(any(Path.class), eq("manga/9/1/page.png"), eq(MediaStyle.SMALL));
    }

    @Test
    void styledNonImageIsRejected() throws Exception {
        Path file = uploadFolder.resolve("games/9/archive.zip");
        Files.createDirectories(file.getParent());
        Files.write(file, "not-an-image".getBytes(StandardCharsets.UTF_8));
        Map<String, String> query = queryOf(signer.sign("games/9/archive.zip"));
        MediaController styledController = new MediaController(
                signer, storageService(), new MediaImageProcessor());

        ResponseEntity<Resource> response = styledController.getMedia(
                query.get("path"), Long.parseLong(query.get("expires")), query.get("signature"), "small");

        assertEquals(415, response.getStatusCode().value());
    }

    @Test
    void processingFailureDoesNotFallBackToTheOriginal() throws Exception {
        Path file = uploadFolder.resolve("manga/9/1/page.png");
        Files.createDirectories(file.getParent());
        Files.write(file, "source".getBytes(StandardCharsets.UTF_8));
        Map<String, String> query = queryOf(signer.sign("manga/9/1/page.png"));
        MediaImageProcessor processor = mock(MediaImageProcessor.class);
        when(processor.process(any(Path.class), eq("manga/9/1/page.png"), eq(MediaStyle.SMALL)))
                .thenThrow(new IOException("test failure"));
        MediaController styledController = new MediaController(signer, storageService(), processor);

        ResponseEntity<Resource> response = styledController.getMedia(
                query.get("path"), Long.parseLong(query.get("expires")), query.get("signature"), "small");

        assertEquals(500, response.getStatusCode().value());
    }

    private FileStorageService storageService() {
        FileStorageService storageService = new FileStorageService();
        ReflectionTestUtils.setField(storageService, "uploadFolder", uploadFolder.toString());
        return storageService;
    }

    private Map<String, String> queryOf(String url) {
        Map<String, String> result = new HashMap<>();
        for (String part : URI.create(url).getRawQuery().split("&")) {
            String[] pair = part.split("=", 2);
            result.put(pair[0], pair[1]);
        }
        return result;
    }
}
