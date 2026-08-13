package com.shiyq.util;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JWTUtilTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-long";

    @Test
    void shouldIssueAndVerifyVersionedToken() {
        JWTUtil jwtUtil = new JWTUtil(SECRET, 1);

        DecodedJWT decodedJWT = jwtUtil.verify(jwtUtil.getLoginToken(7, 3));

        assertEquals(7, decodedJWT.getClaim("userId").asInt());
        assertEquals(3, decodedJWT.getClaim("authVersion").asInt());
        assertEquals("7", decodedJWT.getSubject());
        assertEquals("acg17", decodedJWT.getIssuer());
    }

    @Test
    void shouldRejectTokenSignedByAnotherSecret() {
        JWTUtil issuer = new JWTUtil(SECRET, 1);
        JWTUtil verifier = new JWTUtil("another-test-secret-with-at-least-32-bytes", 1);

        assertThrows(JWTVerificationException.class,
                () -> verifier.verify(issuer.getLoginToken(7, 3)));
    }

    @Test
    void shouldRejectShortSecret() {
        assertThrows(IllegalArgumentException.class, () -> new JWTUtil("too-short", 1));
    }
}
