package com.shiyq.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Component
public class JWTUtil {

    private static final String ISSUER = "acg17";

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expirationMillis;

    public JWTUtil(@Value("${security.jwt.secret}") String signature,
                   @Value("${security.jwt.expiration-hours:24}") long expirationHours) {
        if (signature == null || signature.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }
        if (expirationHours <= 0) {
            throw new IllegalArgumentException("JWT expiration hours must be positive");
        }
        this.algorithm = Algorithm.HMAC256(signature);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        this.expirationMillis = TimeUnit.HOURS.toMillis(expirationHours);
    }

    /**
     * 针对网页登录用户，获取token
     */
    public String getLoginToken(Integer userId, Integer authVersion) {
        Date issuedAt = new Date();
        Date expiresAt = new Date(issuedAt.getTime() + expirationMillis);
        return JWT.create()
                .withIssuer(ISSUER)
                .withSubject(userId.toString())
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .withClaim("userId", userId)
                .withClaim("authVersion", authVersion)
                .sign(algorithm);
    }

    /**
     * 验证token
     * @param token 待验证token
     * @return payload存储的值
     */
    public DecodedJWT verify(String token) {
        return verifier.verify(token);
    }

}
