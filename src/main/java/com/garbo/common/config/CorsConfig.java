package com.garbo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow Flutter web dev servers and local frontend origins during development.
        // Restrict this later for production deployment.
        config.addAllowedOriginPattern("*");

        // Allow all headers
        config.addAllowedHeader("*");
        
        // Allow all HTTP methods
        config.addAllowedMethod("*");
        
        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
