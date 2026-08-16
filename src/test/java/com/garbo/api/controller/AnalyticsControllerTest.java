package com.garbo.api.controller;

import com.garbo.api.dto.Collect_analyze_dtos.DashboardResponseDTO;
import com.garbo.core.service.AnalyticsService;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
class AnalyticsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    AnalyticsService service;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @MockBean
    JwtUtil jwtUtil;

    @Test
    @WithMockUser
    void getDashboard_success_returns200() throws Exception {
        when(service.getDashboard("DAY", "KMC"))
                .thenReturn(new DashboardResponseDTO(10, 8, 2, Collections.emptyList()));

        mvc.perform(get("/api/admin/analytics")
                        .param("filter", "DAY")
                        .param("council", "KMC"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getDashboard_error_returns500() throws Exception {
        when(service.getDashboard("DAY", "KMC")).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/admin/analytics")
                        .param("filter", "DAY")
                        .param("council", "KMC"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("ERROR: boom")));
    }
}
