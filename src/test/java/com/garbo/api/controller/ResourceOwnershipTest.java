package com.garbo.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.controller.citizen.CitizenController;
import com.garbo.core.entity.User;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.service.CollectorPerformanceService;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.UserGamificationTaskService;
import com.garbo.core.service.UserService;
import com.garbo.core.service.shared.CollectionRequestService;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, CitizenController.class})
class ResourceOwnershipTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserGamificationTaskService userGamificationTaskService;

    @MockBean
    private CollectorPerformanceService collectorPerformanceService;

    @MockBean
    private CollectionRequestService collectionRequestService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        // Initialize CurrentUserService static reference via MockBean
        new CurrentUserService(userRepository);
    }

    @Test
    @WithMockUser(username = "owner@garbo.local", roles = "CITIZEN")
    void allowsOwnerToUpdateProfile() throws Exception {
        User owner = new User();
        owner.setEmpId(123L);
        owner.setEmail("owner@garbo.local");
        owner.setRole("CITIZEN");

        when(userRepository.findFirstByEmailIgnoreCase("owner@garbo.local")).thenReturn(Optional.of(owner));
        when(userService.updateUser(123L, owner)).thenReturn(Optional.of(owner));

        mvc.perform(put("/api/users/123")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(owner)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "intruder@garbo.local", roles = "CITIZEN")
    void blocksIntruderFromUpdatingOtherUserProfile() throws Exception {
        User intruder = new User();
        intruder.setEmpId(999L);
        intruder.setEmail("intruder@garbo.local");
        intruder.setRole("CITIZEN");

        User target = new User();
        target.setEmpId(123L);
        target.setEmail("owner@garbo.local");

        when(userRepository.findFirstByEmailIgnoreCase("intruder@garbo.local")).thenReturn(Optional.of(intruder));

        mvc.perform(put("/api/users/123")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(target)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@garbo.local", roles = "ADMIN")
    void allowsAdminToUpdateUserProfile() throws Exception {
        User admin = new User();
        admin.setEmpId(456L);
        admin.setEmail("admin@garbo.local");
        admin.setRole("ADMIN");

        User target = new User();
        target.setEmpId(123L);
        target.setEmail("owner@garbo.local");

        when(userRepository.findFirstByEmailIgnoreCase("admin@garbo.local")).thenReturn(Optional.of(admin));
        when(userService.updateUser(123L, target)).thenReturn(Optional.of(target));

        mvc.perform(put("/api/users/123")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(target)))
                .andExpect(status().isOk());
    }
}
