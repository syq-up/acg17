package com.shiyq.service;

import app.photofox.vipsffm.VImage;
import app.photofox.vipsffm.Vips;
import app.photofox.vipsffm.VipsOption;
import app.photofox.vipsffm.enums.VipsSize;
import com.shiyq.util.ImageFileInspector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates the small fixed set of on-demand image variants.
 *
 * <p>This service intentionally does not participate in upload thumbnail
 * generation.  A variant is created only when a signed media URL requests it,
 * and is then retained in {@code {uploadFolder}/media-cache}.</p>
 */
@Service
public class MediaImageProcessor {

    static final String CACHE_FOLDER = "media-cache";
    private static final String WEBP_SUFFIX = ".webp";
    private static final MediaType WEBP_MEDIA_TYPE = MediaType.parseMediaType("image/webp");

    /**
     * The cache is deliberately simple.  It prevents duplicate first renders
     * within this application process; the atomic move also makes a result
     * safe if more than one process happens to render the same key.
     */
    private final ConcurrentHashMap<String, Object> renderLocks = new ConcurrentHashMap<>();

    @Value("${file.uploadFolder}")
    private String uploadFolder;

    private final Path uploadRootOverride;

    public MediaImageProcessor() {
        this.uploadRootOverride = null;
    }

    MediaImageProcessor(Path uploadRoot) {
        this.uploadRootOverride = uploadRoot == null ? null : uploadRoot.toAbsolutePath().normalize();
    }

    /**
     * Returns the source itself when no processing is required, or a static
     * WebP variant when the selected rule requires downsizing.
     */
    public MediaVariant process(Path source, String relativePath, MediaStyle style) throws IOException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(relativePath, "relativePath");
        Objects.requireNonNull(style, "style");

        if (style == MediaStyle.ORIGINAL) {
            return MediaVariant.original(source);
        }

        final ImageFileInspector.ImageFileInfo imageInfo;
        try {
            // ImageIO is used only for the cheap header/dimension decision.  It
            // also means small animated images remain animated and byte-for-
            // byte unchanged, as required by the style contract.
            imageInfo = ImageFileInspector.inspect(source.toFile());
        } catch (IOException exception) {
            throw new UnsupportedImageException("媒体文件不是受支持的图片", exception);
        }

        if (!style.requiresProcessing(imageInfo.width(), imageInfo.height())) {
            return MediaVariant.original(source);
        }

        Path cacheRoot = cacheRoot();
        Files.createDirectories(cacheRoot);
        CacheKey cacheKey = CacheKey.create(relativePath, style, source);
        Path cached = cacheRoot.resolve(cacheKey.value() + WEBP_SUFFIX);
        Object lock = renderLocks.computeIfAbsent(cacheKey.value(), ignored -> new Object());
        try {
            synchronized (lock) {
                if (Files.isRegularFile(cached) && Files.size(cached) > 0) {
                    return MediaVariant.derived(cached);
                }

                Path temporary = Files.createTempFile(cacheRoot, "." + cacheKey.value() + "-", ".tmp");
                try {
                    renderWebp(source, temporary, style.maxEdge());
                    if (!Files.isRegularFile(temporary) || Files.size(temporary) == 0) {
                        throw new IOException("媒体图片处理未生成有效文件");
                    }
                    installAtomically(temporary, cached);
                } finally {
                    Files.deleteIfExists(temporary);
                }
                return MediaVariant.derived(cached);
            }
        } finally {
            // Remove only after releasing the monitor.  A waiter that already
            // acquired the old lock must be allowed to observe the installed
            // file before a later request can create a replacement lock.
            renderLocks.remove(cacheKey.value(), lock);
        }
    }

    void renderWebp(Path source, Path temporary, int maxEdge) throws IOException {
        try {
            Vips.run(arena -> {
                VImage thumbnail = VImage.thumbnail(
                        arena,
                        source.toAbsolutePath().toString(),
                        maxEdge,
                        VipsOption.Int("height", maxEdge),
                        VipsOption.Enum("size", VipsSize.SIZE_DOWN));
                thumbnail.webpsave(
                        temporary.toAbsolutePath().toString(),
                        VipsOption.Int("Q", 90),
                        VipsOption.Int("effort", 4),
                        VipsOption.Int("alpha-q", 100),
                        VipsOption.Boolean("smart-subsample", true),
                        // VipsForeignKeep.NONE has the stable raw value 0 in
                        // supported libvips versions.  This avoids relying on
                        // a generated enum which is absent in vips-ffm 1.9.8.
                        VipsOption.Enum("keep", 0));
            });
        } catch (RuntimeException exception) {
            throw new IOException("实时媒体图片处理失败", exception);
        }
    }

    private void installAtomically(Path temporary, Path cached) throws IOException {
        try {
            Files.move(temporary, cached,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, cached, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path cacheRoot() throws IOException {
        Path root = uploadRootOverride;
        if (root == null) {
            if (uploadFolder == null || uploadFolder.isBlank()) {
                throw new IOException("未配置媒体上传目录");
            }
            root = Path.of(uploadFolder).toAbsolutePath().normalize();
        }
        return root.resolve(CACHE_FOLDER).normalize();
    }

    public record MediaVariant(Path path, MediaType mediaType) {
        static MediaVariant original(Path path) {
            return new MediaVariant(path, null);
        }

        static MediaVariant derived(Path path) {
            return new MediaVariant(path, WEBP_MEDIA_TYPE);
        }

        public boolean isDerived() {
            return mediaType != null;
        }
    }

    public static class UnsupportedImageException extends IOException {
        public UnsupportedImageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record CacheKey(String value) {
        static CacheKey create(String relativePath, MediaStyle style, Path source) throws IOException {
            String input = relativePath + '\0'
                    + style.name() + '\0'
                    + Files.size(source) + '\0'
                    + Files.getLastModifiedTime(source).toMillis();
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256")
                        .digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return new CacheKey(HexFormat.of().formatHex(digest));
            } catch (NoSuchAlgorithmException exception) {
                throw new IOException("无法计算媒体缓存键", exception);
            }
        }
    }
}
