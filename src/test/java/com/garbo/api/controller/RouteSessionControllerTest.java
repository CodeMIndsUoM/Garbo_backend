package com.garbo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.AutoRoutePreviewRequestDTO;
import com.garbo.api.dto.RouteAssignmentRequestDTO;
import com.garbo.core.service.route.AutoRouteService;
import com.garbo.core.service.route.RouteAssignmentService;
import com.garbo.core.service.route.RouteSessionService;
import com.garbo.core.repository.RouteAssignmentRepository;
import com.garbo.core.repository.RouteVehicleRouteRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(controllers = RouteSessionController.class)
public class RouteSessionControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @MockBean RouteSessionService routeSessionService;
    @MockBean RouteAssignmentService routeAssignmentService;
    @MockBean AutoRouteService autoRouteService;
    @MockBean RouteAssignmentRepository assignmentRepository;
    @MockBean RouteVehicleRouteRepository vehicleRouteRepository;
    @MockBean UserRepository userRepository;

    // Required so Spring can construct JwtAuthenticationFilter (part of the
    // security filter chain, which @WebMvcTest still applies) without
    // needing the real service/database behind it.
    @MockBean CustomUserDetailsService customUserDetailsService;
    @MockBean JwtUtil jwtUtil;

    @Test
    @WithMockUser
    void autoPreview_missingCouncil_returnsBadRequest() throws Exception {
        AutoRoutePreviewRequestDTO body = new AutoRoutePreviewRequestDTO();
        body.setCouncil("");
        mvc.perform(post("/api/route-sessions/auto-preview")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("council is required"));
    }

    @Test
    @WithMockUser
    void createRouteSession_missingDepot_returnsBadRequest() throws Exception {
        RouteAssignmentRequestDTO req = new RouteAssignmentRequestDTO();
        req.setSelectedBinIds(java.util.List.of(1L));
        req.setDepotLat(0.0);
        req.setDepotLng(0.0);
        mvc.perform(post("/api/route-sessions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("depotLat and depotLng are required"));
    }

    @Test
    @WithMockUser
    void getSnapshot_notFound_returns404() throws Exception {
        when(routeSessionService.getLatestSnapshot(any(UUID.class))).thenThrow(new IllegalArgumentException("not found"));
        mvc.perform(get("/api/route-sessions/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void collectBin_returnsNotUpdated_whenServiceFalse() throws Exception {
        UUID sid = UUID.randomUUID();
        when(routeAssignmentService.markBinCollected(any(), anyLong())).thenReturn(false);
        mvc.perform(patch("/api/route-sessions/" + sid + "/bins/5/collect")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_UPDATED"));
    }
}