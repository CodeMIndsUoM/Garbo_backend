package com.garbo.infrastructure.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // Strong secret key, use a longer random string in production
    private final String SECRET = "my_super_secret_key_my_super_secret_key"; // at least 256-bit for HS256

    private final long EXPIRATION = 24 * 60 * 60 * 1000; // 24 hours

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    // Generate JWT token
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
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
        String extractedUsername = extractUsername(token);

        boolean isSameUser = extractedUsername.equalsIgnoreCase(username);
        boolean isExpired = isTokenExpired(token);

        return isSameUser && !isExpired;
    }

    // Check if expired
    private boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }

    private Claims getClaims(String token) {
        // Use parserBuilder to be compatible with newer jjwt versions
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
