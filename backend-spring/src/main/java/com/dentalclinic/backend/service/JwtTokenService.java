package com.dentalclinic.backend.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JwtTokenService {

    private final String jwtSecret;
    private final long expiresMinutes;

    public JwtTokenService(
            @Value("${security.jwt.secret}") String jwtSecret,
            @Value("${security.jwt.expires-minutes}") long expiresMinutes
    ) {
        this.jwtSecret = jwtSecret;
        this.expiresMinutes = Math.max(1L, expiresMinutes);
    }

    public String createAccessToken(Map<String, Object> loginBody) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiresMinutes, ChronoUnit.MINUTES);

        Integer userId = toInt(loginBody.get("userId"));
        String roleName = toString(loginBody.get("roleName"));
        String loginStatus = toString(loginBody.get("loginStatus"));
        Boolean superAdmin = toBoolean(loginBody.get("superAdmin"));

        return Jwts.builder()
                .subject(userId == null ? "unknown" : String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("userId", userId)
                .claim("roleName", roleName)
                .claim("loginStatus", loginStatus)
                .claim("superAdmin", superAdmin)
                .claim("permissions", toStringList(loginBody.get("permissions")))
                .signWith(signingKey())
                .compact();
    }

    public long expiresMinutes() {
        return expiresMinutes;
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Map<String, Object> parseTokenClaims(String token) {
        Claims claims = parseClaims(token);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("subject", claims.getSubject());
        out.put("issuedAt", claims.getIssuedAt());
        out.put("expiresAt", claims.getExpiration());
        out.put("userId", claims.get("userId"));
        out.put("roleName", claims.get("roleName"));
        out.put("loginStatus", claims.get("loginStatus"));
        out.put("superAdmin", claims.get("superAdmin"));
        out.put("permissions", claims.get("permissions"));
        return out;
    }

    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return null;
        }
        String prefix = "Bearer ";
        if (!authorizationHeader.startsWith(prefix)) {
            return null;
        }
        String token = authorizationHeader.substring(prefix.length()).trim();
        return token.isEmpty() ? null : token;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        Claims claims = parseClaims(token);
        Object raw = claims.get("permissions");
        if (raw instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    private SecretKey signingKey() {
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            bytes = Arrays.copyOf(bytes, 32);
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Integer toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String toString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Boolean toBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value == null) {
            return null;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private List<String> toStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
