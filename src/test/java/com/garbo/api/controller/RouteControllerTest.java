package com.garbo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.RouteRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.service.route.RouteSessionService;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = RouteController.class)
public class RouteControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean RouteSessionService routeSessionService;

    // Required so Spring can construct JwtAuthenticationFilter (part of the
    // security filter chain, which @WebMvcTest still applies) without
    // needing the real service/database behind it.
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    @Test
    @WithMockUser
    void optimize_success_returns200() throws Exception {
        RouteRequestDTO req = new RouteRequestDTO();
        req.setDepotLat(1.0);
        req.setDepotLng(2.0);
        when(routeSessionService.optimizeAndBroadcast(any())).thenReturn(new RouteSessionSnapshotDTO("id", 1L, 1L, "READY", "HTTP_OPTIMIZE", java.util.List.of(), java.util.List.of(), java.util.List.of(), null, null));
        mvc.perform(post("/api/routes/optimize").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void optimize_error_returns500() throws Exception {
        RouteRequestDTO req = new RouteRequestDTO();
        req.setDepotLat(1.0);
        req.setDepotLng(2.0);
        when(routeSessionService.optimizeAndBroadcast(any())).thenThrow(new RuntimeException("boom"));
        mvc.perform(post("/api/routes/optimize").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }
}