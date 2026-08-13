package com.shiyq.controller;

import com.shiyq.service.FileStorageService;
import com.shiyq.service.MediaUrlSigner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * 校验短期签名后读取上传目录中的媒体文件。
 */
@RestController
@RequestMapping("/media")
public class MediaController {

    private final MediaUrlSigner mediaUrlSigner;
    private final FileStorageService fileStorageService;

    public MediaController(MediaUrlSigner mediaUrlSigner, FileStorageService fileStorageService) {
        this.mediaUrlSigner = mediaUrlSigner;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<Resource> getMedia(@RequestParam String path,
                                             @RequestParam long expires,
                                             @RequestParam String signature) {
        final String relativePath;
        try {
            relativePath = mediaUrlSigner.verify(path, expires, signature);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(403)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        final Path mediaFile;
        final long mediaSize;
        try {
            mediaFile = fileStorageService.resolveReadableFile(relativePath);
            mediaSize = Files.size(mediaFile);
        } catch (NoSuchFileException exception) {
            return ResponseEntity.notFound().build();
        } catch (IOException exception) {
            return ResponseEntity.status(403)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        FileSystemResource resource = new FileSystemResource(mediaFile);
        MediaType mediaType = MediaTypeFactory.getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        long maxAge = mediaUrlSigner.remainingSeconds(expires);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(mediaFile.getFileName().toString(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(mediaSize)
                .cacheControl(CacheControl.maxAge(maxAge, TimeUnit.SECONDS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .body(resource);
    }
}
