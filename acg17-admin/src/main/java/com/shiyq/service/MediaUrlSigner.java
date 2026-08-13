package com.shiyq.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;

/**
 * 为上传目录中的文件生成短期访问凭证。
 *
 * <p>签名绑定编码后的相对路径和过期时间。媒体接口只接受由此组件签发、
 * 尚未过期且路径合法的凭证。</p>
 */
@Component
public class MediaUrlSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int MINIMUM_SECRET_BYTES = 32;
    private static final int MAXIMUM_PATH_LENGTH = 2048;

    private final byte[] secret;
    private final long expirationSeconds;
    private final String accessPath;
    private final Clock clock;

    @Autowired
    public MediaUrlSigner(@Value("${security.media.secret}") String signature,
                          @Value("${security.media.expiration-minutes:60}") long expirationMinutes,
                          @Value("${media.access-path:/api/media}") String accessPath) {
        this(signature, expirationMinutes, accessPath, Clock.systemUTC());
    }

    public MediaUrlSigner(String signature, long expirationMinutes, String accessPath, Clock clock) {
        if (signature == null || signature.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalArgumentException("Media URL secret must contain at least 32 bytes");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("Media URL expiration minutes must be positive");
        }
        if (accessPath == null || !accessPath.startsWith("/")) {
            throw new IllegalArgumentException("Media access path must start with '/'");
        }
        this.secret = signature.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = Math.multiplyExact(expirationMinutes, 60L);
        this.accessPath = accessPath.endsWith("/")
                ? accessPath.substring(0, accessPath.length() - 1)
                : accessPath;
        this.clock = clock;
    }

    public String sign(String relativePath) {
        String normalizedPath = normalizeRelativePath(relativePath);
        String encodedPath = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalizedPath.getBytes(StandardCharsets.UTF_8));
        long expires = clock.instant().getEpochSecond() + expirationSeconds;
        String signature = createSignature(encodedPath, expires);
        return accessPath + "?path=" + encodedPath
                + "&expires=" + expires
                + "&signature=" + signature;
    }

    public String sign(String subFolder, String relativePath) {
        if (subFolder == null || relativePath == null) {
            throw new IllegalArgumentException("媒体文件路径不能为空");
        }
        String normalizedFolder = subFolder.trim().replace('\\', '/');
        String normalizedPath = relativePath.trim().replace('\\', '/');
        while (normalizedFolder.endsWith("/")) {
            normalizedFolder = normalizedFolder.substring(0, normalizedFolder.length() - 1);
        }
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return sign(normalizedFolder + "/" + normalizedPath);
    }

    public String verify(String encodedPath, long expires, String suppliedSignature) {
        long now = clock.instant().getEpochSecond();
        if (expires <= now) {
            throw new IllegalArgumentException("媒体访问地址已过期");
        }
        if (encodedPath == null || encodedPath.isEmpty()
                || suppliedSignature == null || suppliedSignature.isEmpty()) {
            throw new IllegalArgumentException("媒体访问签名不完整");
        }

        byte[] suppliedBytes;
        byte[] expectedBytes;
        try {
            suppliedBytes = Base64.getUrlDecoder().decode(suppliedSignature);
            expectedBytes = Base64.getUrlDecoder().decode(createSignature(encodedPath, expires));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("媒体访问签名无效", exception);
        }
        if (!MessageDigest.isEqual(expectedBytes, suppliedBytes)) {
            throw new IllegalArgumentException("媒体访问签名无效");
        }

        try {
            String decodedPath = new String(Base64.getUrlDecoder().decode(encodedPath), StandardCharsets.UTF_8);
            return normalizeRelativePath(decodedPath);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("媒体访问路径无效", exception);
        }
    }

    public long remainingSeconds(long expires) {
        return Math.max(0L, expires - clock.instant().getEpochSecond());
    }

    private String createSignature(String encodedPath, long expires) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] signed = mac.doFinal((encodedPath + "." + expires).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signed);
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成媒体访问签名", exception);
        }
    }

    private String normalizeRelativePath(String relativePath) {
        if (relativePath == null) {
            throw new IllegalArgumentException("媒体文件路径不能为空");
        }
        String normalized = relativePath.trim().replace('\\', '/');
        if (normalized.isEmpty() || normalized.length() > MAXIMUM_PATH_LENGTH
                || normalized.startsWith("/") || normalized.matches("^[A-Za-z]:.*")
                || normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("媒体文件路径无效");
        }
        StringBuilder result = new StringBuilder();
        for (String segment : normalized.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("媒体文件路径无效");
            }
            if (result.length() > 0) {
                result.append('/');
            }
            result.append(segment);
        }
        return result.toString();
    }
}
