package com.garbo.api.controller.adminAnalytics;

import com.garbo.api.dto.binAnalyzeDTOs.BinAnalyticsResponseDTO;
import com.garbo.core.service.AdminAnalytics.BinAnalyticsService;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BinAnalyticsController.class)
class BinAnalyticsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    BinAnalyticsService service;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @MockBean
    JwtUtil jwtUtil;

    @Test
    @WithMockUser
    void getAnalytics_success_returns200() throws Exception {
        when(service.getAnalytics("KMC")).thenReturn(mock(BinAnalyticsResponseDTO.class));

        mvc.perform(get("/api/admin/bin-analytics")
                        .param("councilId", "KMC"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAnalytics_error_returns500() throws Exception {
        when(service.getAnalytics("KMC")).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/admin/bin-analytics")
                        .param("councilId", "KMC"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Error: boom")));
    }
}
