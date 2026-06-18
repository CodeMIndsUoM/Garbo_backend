package com.garbo.core.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.core.entity.UserNotification;
import com.garbo.core.notification.NotificationContext;
import com.garbo.core.notification.NotificationType;
import com.garbo.core.repository.DeviceTokenRepository;
import com.garbo.core.repository.UserNotificationRepository;
import com.garbo.infrastructure.push.FcmPushService;
import com.garbo.infrastructure.websocket.NotificationBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserNotificationRepository notificationRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private NotificationBroadcaster notificationBroadcaster;

    @Mock
    private FcmPushService fcmPushService;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                deviceTokenRepository,
                notificationBroadcaster,
                fcmPushService,
                new ObjectMapper()
        );
    }

    @Test
    void notifyUser_persistsBroadcastsAndPushesFcm() {
        when(notificationRepository.findByUserIdAndSourceEventId(42L, "evt-1"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(UserNotification.class)))
                .thenAnswer(invocation -> {
                    UserNotification row = invocation.getArgument(0);
                    row.setId(UUID.randomUUID());
                    return row;
                });

        NotificationContext context = NotificationContext.of(
                NotificationType.BIN_ASSIGNED,
                "Bin assigned",
                "Bin A1 assigned.",
                Map.of("binId", 7),
                "evt-1"
        );

        notificationService.notifyUser(42L, NotificationType.BIN_ASSIGNED, context);

        ArgumentCaptor<UserNotification> savedCaptor = ArgumentCaptor.forClass(UserNotification.class);
        verify(notificationRepository).save(savedCaptor.capture());
        assertEquals(42L, savedCaptor.getValue().getUserId());
        assertEquals("BIN_ASSIGNED", savedCaptor.getValue().getType());

        verify(notificationBroadcaster).broadcastCreated(any(UserNotification.class), eq(Map.of("binId", 7)));
        verify(fcmPushService).sendNotificationPush(any(UserNotification.class), eq(Map.of("binId", 7)));
    }

    @Test
    void notifyUser_isIdempotentBySourceEventId() {
        UserNotification existing = new UserNotification();
        existing.setId(UUID.randomUUID());
        when(notificationRepository.findByUserIdAndSourceEventId(42L, "evt-1"))
                .thenReturn(Optional.of(existing));

        NotificationContext context = NotificationContext.of(
                NotificationType.ROUTE_ASSIGNED,
                Map.of("sessionId", "abc"),
                "evt-1"
        );

        notificationService.notifyUser(42L, NotificationType.ROUTE_ASSIGNED, context);

        verify(notificationRepository, never()).save(any());
        verify(notificationBroadcaster, never()).broadcastCreated(any(), any());
        verify(fcmPushService, never()).sendNotificationPush(any(), any());
    }

    @Test
    void unreadCount_delegatesToRepository() {
        when(notificationRepository.countByUserIdAndReadFalse(99L)).thenReturn(3L);
        assertEquals(3L, notificationService.unreadCount(99L));
    }

    @Test
    void listNotifications_mapsEntitiesToDto() {
        UserNotification row = new UserNotification();
        row.setId(UUID.randomUUID());
        row.setUserId(1L);
        row.setType("COMPLAINT_SUBMITTED");
        row.setTitle("New complaint");
        row.setBody("Body");
        row.setRead(false);
        row.setPriority("HIGH");
        row.setDataJson("{\"complaintId\":5}");

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(row));
        when(notificationBroadcaster.parseDataJson(row.getDataJson()))
                .thenReturn(Map.of("complaintId", 5));

        var result = notificationService.listNotifications(1L, 20, null, false);

        assertEquals(1, result.size());
        assertEquals("COMPLAINT_SUBMITTED", result.get(0).getType());
        assertTrue(result.get(0).isRead() == false);
    }
}
