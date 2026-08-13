package com.shiyq.controller;

import com.shiyq.service.FileStorageService;
import com.shiyq.service.MediaUrlSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

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
        controller = new MediaController(signer, storageService);
    }

    @Test
    void validSignedUrlReturnsOnlyTheSignedFile() throws Exception {
        byte[] content = "signed-media".getBytes(StandardCharsets.UTF_8);
        Path file = uploadFolder.resolve("manga/9/1/page.png");
        Files.createDirectories(file.getParent());
        Files.write(file, content);
        Map<String, String> query = queryOf(signer.sign("manga/9/1/page.png"));

        ResponseEntity<Resource> response = controller.getMedia(
                query.get("path"), Long.parseLong(query.get("expires")), query.get("signature"));

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
                other.get("path"), Long.parseLong(signed.get("expires")), signed.get("signature"));

        assertEquals(403, response.getStatusCode().value());
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
