package com.garbo.api.controller;

import com.garbo.api.dto.notification.DeviceTokenRequest;
import com.garbo.api.dto.common.ApiResponse;
import com.garbo.core.entity.User;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.notification.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationControllerTest {

    private NotificationController notificationController;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = Mockito.mock(NotificationService.class);
        notificationController = new NotificationController(notificationService);

        com.garbo.core.repository.UserRepository userRepository = Mockito.mock(com.garbo.core.repository.UserRepository.class);
        User current = new User();
        current.setEmpId(42L);
        current.setEmail("citizen@garbo.local");
        when(userRepository.findFirstByEmailIgnoreCase("citizen@garbo.local")).thenReturn(Optional.of(current));
        new CurrentUserService(userRepository);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "citizen@garbo.local",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_CITIZEN"))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void notification_registerDeviceToken_validToken_registersUser() throws Exception {
        doNothing().when(notificationService).registerDeviceToken(eq(42L), eq("token-abc"), any());

        DeviceTokenRequest request = new DeviceTokenRequest();
        request.setToken("token-abc");
        request.setPlatform("android");

        ResponseEntity<ApiResponse<Map<String, String>>> response = notificationController.registerDeviceToken(42L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("registered", response.getBody().getData().get("status"));
    }

    @Test
    void notification_markNotificationRead_validNotificationId_marksRead() throws Exception {
        UUID notificationId = UUID.randomUUID();
        when(notificationService.markRead(notificationId, 42L)).thenReturn(true);

        ResponseEntity<ApiResponse<Map<String, Boolean>>> response = notificationController.markRead(notificationId.toString());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(true, response.getBody().getData().get("read"));
    }
}
