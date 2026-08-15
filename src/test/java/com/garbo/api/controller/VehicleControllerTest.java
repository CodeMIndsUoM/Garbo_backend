package com.garbo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.core.entity.Vehicle;
import com.garbo.core.service.VehicleService;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VehicleController.class)
public class VehicleControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private VehicleService vehicleService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser
    void getAll_success_returns200() throws Exception {
        when(vehicleService.getAll()).thenReturn(List.of());

        mvc.perform(get("/api/vehicles")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void create_success_returns200() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate("AB-1234");
        
        when(vehicleService.create(any(Vehicle.class))).thenReturn(vehicle);

        mvc.perform(post("/api/vehicles")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(vehicle)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void update_success_returns200() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setLicensePlate("XYZ-9876");

        when(vehicleService.update(eq(1L), any(Vehicle.class))).thenReturn(vehicle);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/vehicles/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(vehicle)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateStatus_success_returns200() throws Exception {
        Vehicle vehicle = new Vehicle();
        vehicle.setStatus("in_maintenance");

        when(vehicleService.updateStatus(eq(1L), eq("in_maintenance"))).thenReturn(vehicle);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/vehicles/1/status")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"in_maintenance\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void delete_success_returns200() throws Exception {
        org.mockito.Mockito.doNothing().when(vehicleService).delete(1L);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/vehicles/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
