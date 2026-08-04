package com.werewolf.game.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Lightweight JWT helper for issuing and parsing user tokens.
 * Secret and expiry can be overridden with JWT_SECRET / JWT_EXPIRE_HOURS.
 */
public final class JwtUtil {

    private static final String SECRET = resolveSecret();
    private static final long EXPIRE_MS = resolveExpireMs();
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtUtil() {
    }

    public static String createToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXPIRE_MS))
                .signWith(KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Long parseUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Long.valueOf(claims.getSubject());
    }

    private static String resolveSecret() {
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && envSecret.trim().length() >= 32) {
            return envSecret.trim();
        }
        return "werewolf_game_secret_key_must_be_at_least_32_bytes";
    }

    private static long resolveExpireMs() {
        String envHours = System.getenv("JWT_EXPIRE_HOURS");
        try {
            long hours = envHours == null ? 168 : Long.parseLong(envHours.trim());
            return Math.max(1, hours) * 3600 * 1000;
        } catch (NumberFormatException exception) {
            return 168L * 3600 * 1000;
        }
    }
}