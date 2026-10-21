package com.sleapy.project.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
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
    private long JWT_EXPIRATION;

    /**
     * Generate a JWT token for a user after signup/login
     * @param email the user's email (unique identifier)
     * @param userId the user's ID from database
     * @return JWT token as string
     */
    public String generateToken(String email, Long userId) {
        return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
            .signWith(getSigningKey())
            .compact();
    }

    public boolean verifyAuthorizationHeader(String authorizationHeader) {
        try {
            parseClaims(authorizationHeader);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Refresh a still-valid JWT using the identity claims from its Authorization header.
     *
     * @param authorizationHeader the current Bearer token
     * @return a new JWT with a renewed expiration time
     * @throws JwtException if the token is invalid or expired
     * @throws IllegalArgumentException if the header or required claims are missing
     */
    public String refreshToken(String authorizationHeader) {
        Claims claims = parseClaims(authorizationHeader);
        String email = claims.getSubject();
        Number userId = claims.get("userId", Number.class);

        if (email == null || email.isBlank() || userId == null) {
            throw new IllegalArgumentException("JWT is missing required claims");
        }

        return generateToken(email, userId.longValue());
    }

    private Claims parseClaims(String authorizationHeader) {
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

        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }
}
