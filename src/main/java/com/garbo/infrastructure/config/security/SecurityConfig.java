package com.garbo.infrastructure.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableMethodSecurity
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
                        // allow login endpoints without JWT
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/admin/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/superadmin/login").permitAll()
                    // actual controller paths
                    .requestMatchers(HttpMethod.POST, "/api/admins/login").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/superadmins/login").permitAll()
                        // field-staff bin report workflow must always use JWT
                        .requestMatchers(HttpMethod.POST, "/api/bins/*/report").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bins/*/undo").authenticated()
                        // allow bin operations for dashboard map interaction
                        .requestMatchers(HttpMethod.GET, "/api/bins").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bins/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/bins/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/bins/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/bins/**").permitAll()
                        // allow driver operations
                        .requestMatchers(HttpMethod.GET, "/api/drivers/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/drivers/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/drivers/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/drivers/**").permitAll()
                        // allow vehicle operations
                        .requestMatchers(HttpMethod.GET, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/vehicles/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").permitAll()
                        // allow websocket handshake + SockJS endpoints
                        .requestMatchers("/ws/**").permitAll()
                        // health check for containers and load balancers
                        .requestMatchers("/actuator/health").permitAll()
                        // TEMP: allow route-session testing without JWT
                        .requestMatchers("/api/route-sessions").permitAll()
                        .requestMatchers("/api/route-sessions/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/routes/optimize").permitAll()
                        // allow Spring error endpoint to return real status/details
                        .requestMatchers("/error").permitAll()
                        // analytics (NO JWT)
                        .requestMatchers(HttpMethod.GET, "/api/admin/analytics/**").permitAll()
                        //admin analytics (NO JWT)
                        .requestMatchers("/api/admin/bin-analytics/**").permitAll()
                        // staff analytics (NO JWT)
                        .requestMatchers("/api/admin/staffanalytics").permitAll()
                        // complaint analytics (NO JWT)
                        .requestMatchers("/api/admin/complaintanalytics").permitAll()
                        // third party analytics (NO JWT)
                        .requestMatchers("/api/admin/thirdparty/analyze").permitAll()
                        //bin report analytics (NO JWT)
                        .requestMatchers("/api/admin/bin-reports/analytics").permitAll()
                        //vehicle analytics (NO JWT)
                        .requestMatchers("/api/admin/vehicles/analytics/**").permitAll()
                        // monthly report generation (NO JWT)
                        .requestMatchers("/api/admin/reports/**").permitAll()

                        // third-party collector self-registration (public, no JWT)
                        .requestMatchers("/api/auth/thirdparty-register/**").permitAll()
                        // public council list + citizen registration
                        .requestMatchers(HttpMethod.GET, "/api/councils").permitAll()
                        // mobile app version check (public, no JWT)
                        .requestMatchers(HttpMethod.GET, "/api/app/version").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/app/test-sentry").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                        // validate token endpoint requires authentication
                        .requestMatchers(HttpMethod.GET, "/api/auth/validate").authenticated()
                        // any other auth endpoints
                        .requestMatchers("/api/auth/**").permitAll()
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
