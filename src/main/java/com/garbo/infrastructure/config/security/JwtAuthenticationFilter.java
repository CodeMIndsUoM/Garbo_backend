package com.garbo.infrastructure.config.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(CustomUserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT processing for authentication endpoints
        String path = request.getServletPath();
        System.out.println("JwtAuthenticationFilter: incoming path=" + path);
        if (path != null && path.startsWith("/api/auth/")) {
            System.out.println("JwtAuthenticationFilter: skipping auth filter for " + path);
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        System.out.println("JwtAuthenticationFilter: Authorization header='" + authHeader + "'");

        String username = null;
        String token = null;

        // Check if header contains Bearer token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                // Use JwtUtil to extract username
                username = jwtUtil.extractUsername(token);
                System.out.println("JwtAuthenticationFilter: token found, username=" + username);
            } catch (Exception e) {
                // Invalid token, you can log e if needed
                System.out.println("JwtAuthenticationFilter: token parse error: " + e.getMessage());
            }
        }

        // If username exists and user is not authenticated yet
        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // Validate token matches username
            if (jwtUtil.isTokenValid(token, userDetails.getUsername())) {

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails,
                        null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
