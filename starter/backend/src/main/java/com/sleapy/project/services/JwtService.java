package com.sleapy.project.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import com.sleapy.project.mappers.ClientMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service 
public class JwtService {
    private final ClientMapper clientMapper;

    public JwtService(ClientMapper clientMapper) {
        this.clientMapper = clientMapper;
    }

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
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return false;
        }

        String token = authorizationHeader;
        if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = authorizationHeader.substring(7).trim();
        }

        if (token.isEmpty()) {
            return false;
        }

        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public Optional<Long> getUserId(String authorizationHeader) {
        if (!verifyAuthorizationHeader(authorizationHeader)) {
            return Optional.empty();
        }

        String token = authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)
            ? authorizationHeader.substring(7).trim()
            : authorizationHeader;
        try {
            Object userId = Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().get("userId");
            return userId instanceof Number number ? Optional.of(number.longValue()) : Optional.empty();
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public boolean isAdminAuthorizationHeader(String authorizationHeader) {
        return getUserId(authorizationHeader)
                .flatMap(clientMapper::findById)
                .map(client -> client.isAdmin())
                .orElse(false);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }
}
