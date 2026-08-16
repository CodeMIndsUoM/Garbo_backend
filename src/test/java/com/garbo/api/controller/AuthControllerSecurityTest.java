package com.garbo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.core.entity.Citizen;
import com.garbo.core.service.CitizenService;
import com.garbo.core.service.UserService;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtAuthenticationFilter;
import com.garbo.infrastructure.config.security.JwtUtil;
import com.garbo.infrastructure.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private UserService userService;

    @MockBean
    private CitizenService citizenService;

    @Test
    void login_validCredentials_returnsJwtAndRole() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("user@garbo.local", "secret"));
        when(customUserDetailsService.loadUserByUsername("user@garbo.local"))
                .thenReturn(User.withUsername("user@garbo.local").password("encoded").authorities("ROLE_CITIZEN").build());

        Citizen citizen = new Citizen();
        citizen.setEmpId(42L);
        citizen.setEmail("user@garbo.local");
        citizen.setRole("CITIZEN");
        citizen.setCouncil("KMC");
        citizen.setMustChangePassword(false);

        when(userService.getByEmail("user@garbo.local")).thenReturn(Optional.of(citizen));
        when(jwtUtil.generateToken(eq("user@garbo.local"), eq("citizen"), eq("KMC"))).thenReturn("jwt-token");

        Map<String, String> payload = Map.of(
                "email", "user@garbo.local",
                "password", "secret"
        );

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("citizen"))
                .andExpect(jsonPath("$.email").value("user@garbo.local"));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new AuthenticationException("bad credentials") {
                });

        Map<String, String> payload = Map.of(
                "email", "missing@garbo.local",
                "password", "wrong"
        );

        mvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void validateToken_validJwt_returnsOk() throws Exception {
        when(jwtUtil.extractUsername("valid.jwt.token")).thenReturn("user@garbo.local");
        when(customUserDetailsService.loadUserByUsername("user@garbo.local"))
                .thenReturn(User.withUsername("user@garbo.local").password("encoded").authorities("ROLE_CITIZEN").build());
        when(jwtUtil.isTokenValid("valid.jwt.token", "user@garbo.local")).thenReturn(true);

        mvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    void jwt_invalidToken_isRejectedByFilter() throws Exception {
        when(jwtUtil.extractUsername("bad.token")).thenThrow(new RuntimeException("token parse failed"));

        mvc.perform(get("/api/auth/validate")
                        .header("Authorization", "Bearer bad.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_missingToken_returns401() throws Exception {
        mvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized());
    }
}
