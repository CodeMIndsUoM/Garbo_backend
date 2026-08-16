package com.garbo.api.controller.adminAnalytics;

import com.garbo.api.dto.VehicleAnalyticsDTOs.VehicleAnalyticsDTO;
import com.garbo.core.service.AdminAnalytics.VehicleAnalyticsService;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VehicleAnalyticsController.class)
class VehicleAnalyticsControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    VehicleAnalyticsService vehicleAnalyticsService;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @MockBean
    JwtUtil jwtUtil;

    @Test
    @WithMockUser
    void getAnalytics_success_returns200() throws Exception {
        when(vehicleAnalyticsService.getAnalytics("KMC")).thenReturn(mock(VehicleAnalyticsDTO.class));

        mvc.perform(get("/api/admin/vehicles/analytics")
                        .param("councilId", "KMC"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAnalytics_error_returns500WithJsonError() throws Exception {
        when(vehicleAnalyticsService.getAnalytics(anyString())).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/admin/vehicles/analytics")
                        .param("councilId", "KMC"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("RuntimeException"));
    }

    @Test
    @WithMockUser
    void getAnalyticsByStatus_success_returns200() throws Exception {
        when(vehicleAnalyticsService.getAnalyticsByStatus("all", "KMC")).thenReturn(mock(VehicleAnalyticsDTO.class));

        mvc.perform(get("/api/admin/vehicles/analytics/filter")
                        .param("status", "all")
                        .param("councilId", "KMC"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void getAnalyticsByStatus_error_returns500WithJsonError() throws Exception {
        when(vehicleAnalyticsService.getAnalyticsByStatus(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/admin/vehicles/analytics/filter")
                        .param("status", "available")
                        .param("councilId", "KMC"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("RuntimeException"));
    }
}
