package com.garbo.api.controller;

import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.User;
import com.garbo.core.service.CollectorPerformanceService;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.UserGamificationTaskService;
import com.garbo.core.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerCrudTest {

    private UserController userController;
    private UserService userService;

    private UserGamificationTaskService userGamificationTaskService;

    private CollectorPerformanceService collectorPerformanceService;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);
        userGamificationTaskService = Mockito.mock(UserGamificationTaskService.class);
        collectorPerformanceService = Mockito.mock(CollectorPerformanceService.class);
        userController = new UserController(userService);
        org.springframework.test.util.ReflectionTestUtils.setField(userController, "userGamificationTaskService", userGamificationTaskService);
        org.springframework.test.util.ReflectionTestUtils.setField(userController, "collectorPerformanceService", collectorPerformanceService);

        // Initialize CurrentUserService static repository reference for auth helper methods.
        new CurrentUserService(Mockito.mock(com.garbo.core.repository.UserRepository.class));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void admin_createAdmin_superadminOnly_returns403() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "admin@garbo.local",
                        "secret",
                        AuthorityUtils.createAuthorityList("ROLE_ADMIN")
                )
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("council", "KMC");
        payload.put("fullName", "City Admin");
        payload.put("email", "admin@garbo.local");
        payload.put("contactNumber", "0771234567");

        ResponseEntity<?> response = userController.createUser(payload);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals("Only superadmin can create admins", body.get("message"));
    }

    @Test
    void createUser_validRequest_returnsCreatedUser() throws Exception {
        User saved = new User();
        saved.setEmpId(10L);
        saved.setEmpName("Citizen One");
        saved.setEmail("citizen@garbo.local");
        saved.setRole("CITIZEN");

        when(userService.saveUser(any(User.class))).thenReturn(saved);

        Map<String, Object> payload = new HashMap<>();
        payload.put("empName", "Citizen One");
        payload.put("email", "citizen@garbo.local");
        payload.put("role", "CITIZEN");

        ResponseEntity<?> response = userController.createUser(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("success"));
        User data = (User) body.get("data");
        assertEquals("citizen@garbo.local", data.getEmail());
    }

    @Test
    void getUser_existingUser_returnsUser() throws Exception {
        User user = new User();
        user.setEmpId(11L);
        user.setEmpName("Alice");
        user.setEmail("alice@garbo.local");
        user.setRole("CITIZEN");

        when(userService.getById(11L)).thenReturn(Optional.of(user));

    ResponseEntity<?> response = userController.getUser(11L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertNotNull(body);
    User data = (User) body.get("data");
    assertEquals(11L, data.getEmpId());
    assertEquals("alice@garbo.local", data.getEmail());
    }

    @Test
    void deleteUser_existingUser_deletesUser() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(
            "admin@garbo.local",
            "secret",
            AuthorityUtils.createAuthorityList("ROLE_ADMIN")
        )
    );
        when(userService.deleteUser(12L)).thenReturn(true);

    ResponseEntity<?> response = userController.deleteUser(12L);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    @SuppressWarnings("unchecked")
    Map<String, Object> body = (Map<String, Object>) response.getBody();
    assertNotNull(body);
    assertEquals("User deleted", body.get("message"));
    }
}
