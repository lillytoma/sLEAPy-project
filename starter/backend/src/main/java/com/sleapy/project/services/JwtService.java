package com.sleapy.project.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.expiration}")
    private long ACCESS_TOKEN_EXPIRATION;

    @Value("${jwt.refresh-expiration}")
    private long REFRESH_TOKEN_EXPIRATION;

    public String generateToken(String email, Long userId) {
        return generateAccessToken(email, userId);
    }

    public String generateAccessToken(String email, Long userId) {
        return generateToken(email, userId, "access", ACCESS_TOKEN_EXPIRATION);
    }

    public String generateRefreshToken(String email, Long userId) {
        return generateToken(email, userId, "refresh", REFRESH_TOKEN_EXPIRATION);
    }

    private String generateToken(String email, Long userId, String tokenType, long expiration) {
        return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .claim("type", tokenType)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    public boolean verifyAuthorizationHeader(String authorizationHeader) {
        try {
            parseClaims(authorizationHeader, "access");
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public String refreshToken(String authorizationHeader) {
        Claims claims = parseClaims(authorizationHeader, "refresh");
        String email = claims.getSubject();
        Number userId = claims.get("userId", Number.class);

        if (email == null || email.isBlank() || userId == null) {
            throw new IllegalArgumentException("JWT is missing required claims");
        }

        return generateAccessToken(email, userId.longValue());
    }

    private Claims parseClaims(String authorizationHeader, String expectedType) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }

        String token = authorizationHeader;
        if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorizationHeader.substring(7).trim();
        }

        if (token.isEmpty()) {
            throw new IllegalArgumentException("Bearer token is required");
        }

        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        if (!expectedType.equals(claims.get("type", String.class))) {
            throw new IllegalArgumentException("Invalid token type");
        }

        return claims;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }
}
