package com.sleapy.project.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
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
        SecretKey key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

        return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + JWT_EXPIRATION))
            .signWith(key)
            .compact();
    }
}
