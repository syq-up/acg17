package com.shiyq.controller;

import com.shiyq.service.FileStorageService;
import com.shiyq.service.MediaImageProcessor;
import com.shiyq.service.MediaStyle;
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
    private final MediaImageProcessor mediaImageProcessor;

    public MediaController(MediaUrlSigner mediaUrlSigner,
                           FileStorageService fileStorageService,
                           MediaImageProcessor mediaImageProcessor) {
        this.mediaUrlSigner = mediaUrlSigner;
        this.fileStorageService = fileStorageService;
        this.mediaImageProcessor = mediaImageProcessor;
    }

    @GetMapping
    public ResponseEntity<Resource> getMedia(@RequestParam String path,
                                             @RequestParam long expires,
                                             @RequestParam String signature,
                                             @RequestParam(required = false) String style) {
        final MediaStyle mediaStyle;
        try {
            mediaStyle = MediaStyle.parse(style == null ? "original" : style);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        final String relativePath;
        try {
            relativePath = mediaUrlSigner.verify(path, expires, signature);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(403)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        final Path mediaFile;
        try {
            mediaFile = fileStorageService.resolveReadableFile(relativePath);
        } catch (NoSuchFileException exception) {
            return ResponseEntity.notFound().build();
        } catch (IOException exception) {
            return ResponseEntity.status(403)
                    .cacheControl(CacheControl.noStore())
                    .build();
        }

        Path responseFile = mediaFile;
        boolean derived = false;
        MediaType mediaType = MediaTypeFactory.getMediaType(new FileSystemResource(mediaFile))
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        if (mediaStyle != MediaStyle.ORIGINAL) {
            try {
                MediaImageProcessor.MediaVariant variant = mediaImageProcessor.process(
                        mediaFile, relativePath, mediaStyle);
                responseFile = variant.path();
                derived = variant.isDerived();
                if (variant.mediaType() != null) {
                    mediaType = variant.mediaType();
                }
            } catch (MediaImageProcessor.UnsupportedImageException exception) {
                return ResponseEntity.status(415)
                        .cacheControl(CacheControl.noStore())
                        .build();
            } catch (IOException exception) {
                // A processing error must not silently turn into an original
                // image response: callers should see a failed variant request.
                return ResponseEntity.internalServerError()
                        .cacheControl(CacheControl.noStore())
                        .build();
            }
        }

        FileSystemResource resource = new FileSystemResource(responseFile);
        final long responseSize;
        try {
            responseSize = Files.size(responseFile);
        } catch (IOException exception) {
            return ResponseEntity.internalServerError()
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
        long maxAge = mediaUrlSigner.remainingSeconds(expires);
        String filename = mediaFile.getFileName().toString();
        if (derived) {
            int extensionIndex = filename.lastIndexOf('.');
            filename = (extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename) + ".webp";
        }
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(responseSize)
                .cacheControl(CacheControl.maxAge(maxAge, TimeUnit.SECONDS).cachePrivate())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .header("Referrer-Policy", "no-referrer")
                .body(resource);
    }
}
