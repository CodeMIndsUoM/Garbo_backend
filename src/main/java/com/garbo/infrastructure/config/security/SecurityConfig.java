package com.garbo.infrastructure.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
            JwtAuthenticationFilter jwtFilter,
            CorsConfigurationSource corsConfigurationSource) {
        this.userDetailsService = userDetailsService;
        this.jwtFilter = jwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authBuilder.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
        return authBuilder.build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        // allow CORS preflight
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // allow login POST explicitly
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        // any other auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        // change for testing.
                        // .anyRequest().permitAll()
                        .anyRequest().authenticated() // all others need JWT
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authEx) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            String msg = "AUTHENTICATION_ENTRY_POINT: " + authEx.getMessage();
                            System.out.println(msg + " — Request: method=" + request.getMethod() + " uri="
                                    + request.getRequestURI());
                            // log some headers for debugging
                            String authHeader = request.getHeader("Authorization");
                            String origin = request.getHeader("Origin");
                            System.out.println("Headers: Authorization=" + authHeader + ", Origin=" + origin);
                            response.getWriter().write("{\"error\":\"" + msg + "\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedEx) -> {
                            response.setStatus(403);
                            response.setContentType("application/json");
                            String msg = "ACCESS_DENIED: " + accessDeniedEx.getMessage();
                            System.out.println(msg + " — Request: method=" + request.getMethod() + " uri="
                                    + request.getRequestURI());
                            String authHeader = request.getHeader("Authorization");
                            String origin = request.getHeader("Origin");
                            System.out.println("Headers: Authorization=" + authHeader + ", Origin=" + origin);
                            response.getWriter().write("{\"error\":\"" + msg + "\"}");
                        }))
                // register DaoAuthenticationProvider so Spring Security can authenticate
                .authenticationProvider(daoAuthenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        // Disable HTTP Basic and form login for API
        http.httpBasic(basic -> basic.disable());
        http.formLogin(form -> form.disable());

        return http.build();
    }
}
