package com.garbo.infrastructure.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private final String secret;
    private final long expiration = 24 * 60 * 60 * 1000; // 24 hours

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("jwt.secret must be at least 32 characters");
        }
        this.secret = secret;
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate JWT token
    public String generateToken(String username, String role) {
        return generateToken(username, role, null);
    }

    public String generateToken(String username, String role, String council) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration));
        if (council != null && !council.isBlank()) {
            builder.claim("council", council.trim());
        }
        return builder.signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractCouncil(String token) {
        Object council = getClaims(token).get("council");
        return council == null ? null : council.toString();
    }

    // Extract username from JWT
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    // Extract role from JWT
    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    // Validate token (checks expiration)
    public boolean isTokenValid(String token, String username) {
        return extractUsername(token).equalsIgnoreCase(username) && !isTokenExpired(token);
    }

    // Check if expired
    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
