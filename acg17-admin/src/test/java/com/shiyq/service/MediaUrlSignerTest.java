package com.shiyq.service;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MediaUrlSignerTest {

    private static final String SECRET = "test-media-secret-with-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-08-13T12:00:00Z");

    @Test
    void signedUrlRoundTripsTheManagedPath() {
        MediaUrlSigner signer = signerAt(NOW);

        Map<String, String> query = queryOf(signer.sign("manga/", "9/1/page.png"));

        assertEquals("manga/9/1/page.png", signer.verify(
                query.get("path"), Long.parseLong(query.get("expires")), query.get("signature")));
    }

    @Test
    void changingThePathInvalidatesTheSignature() {
        MediaUrlSigner signer = signerAt(NOW);
        Map<String, String> original = queryOf(signer.sign("manga/9/1/page.png"));
        Map<String, String> another = queryOf(signer.sign("manga/10/1/page.png"));

        assertThrows(IllegalArgumentException.class, () -> signer.verify(
                another.get("path"), Long.parseLong(original.get("expires")), original.get("signature")));
    }

    @Test
    void expiredUrlIsRejected() {
        MediaUrlSigner issuer = signerAt(NOW);
        Map<String, String> query = queryOf(issuer.sign("illustrations/upload/image.png"));
        MediaUrlSigner verifier = signerAt(NOW.plusSeconds(61));

        assertThrows(IllegalArgumentException.class, () -> verifier.verify(
                query.get("path"), Long.parseLong(query.get("expires")), query.get("signature")));
    }

    @Test
    void traversalPathCannotBeSigned() {
        MediaUrlSigner signer = signerAt(NOW);

        assertThrows(IllegalArgumentException.class, () -> signer.sign("manga/../avatar/private.png"));
        assertThrows(IllegalArgumentException.class, () -> signer.sign("/etc/passwd"));
    }

    private MediaUrlSigner signerAt(Instant instant) {
        return new MediaUrlSigner(SECRET, 1, "/api/media", Clock.fixed(instant, ZoneOffset.UTC));
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
