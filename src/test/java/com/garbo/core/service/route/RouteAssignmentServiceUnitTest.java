package com.garbo.core.service.route;

import com.garbo.api.dto.RouteAssignmentRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.entity.RouteBinStop;
import com.garbo.core.entity.RouteVehicleRoute;
import com.garbo.core.repository.*;
import com.garbo.core.service.field_staff.BinService;
import com.garbo.core.service.notification.NotificationPublisher;
import com.garbo.infrastructure.websocket.RouteCollectionBroadcaster;
import com.garbo.infrastructure.websocket.TaskAlertBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.garbo.infrastructure.websocket.CouncilBinStompBroadcaster;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class RouteAssignmentServiceUnitTest {

    @Mock RouteSessionRepository routeSessionRepository;
    @Mock RouteAssignmentRepository routeAssignmentRepository;
    @Mock RouteVehicleRouteRepository vehicleRouteRepository;
    @Mock RouteBinStopRepository binStopRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock BinCollectorRepository collectorRepository;
    @Mock BinRepository binRepository;
    @Mock UserRepository userRepository;
    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock CouncilBinStompBroadcaster councilBinStompBroadcaster;
    
    @Mock BinService binService;
    @Mock TaskAlertBroadcaster taskAlertBroadcaster;
    @Mock NotificationPublisher notificationPublisher;
    @Mock RouteCollectionBroadcaster routeCollectionBroadcaster;

    RouteAssignmentService service;

    @BeforeEach
    void setup() {
       
        service = new RouteAssignmentService(
                routeSessionRepository,
                routeAssignmentRepository,
                vehicleRouteRepository,
                binStopRepository,
                vehicleRepository,
                collectorRepository,
                binRepository,
                userRepository,
            routeCollectionBroadcaster,
                binService,
                taskAlertBroadcaster,
                notificationPublisher
        );
    }

    @Test
    public void markBinCollected_idempotent_behaviour() {
        UUID sessionId = UUID.randomUUID();
        Long binId = 123L;
        RouteBinStop stop = new RouteBinStop(); stop.setId(1L); stop.setBinId(binId); stop.setStatus("PENDING");

        when(binStopRepository.findBySessionIdAndBinId(eq(sessionId), eq(binId))).thenReturn(Optional.of(stop));
        when(binStopRepository.markCollected(eq(1L), any(LocalDateTime.class))).thenReturn(1).thenReturn(0);

        boolean first = service.markBinCollected(sessionId, binId);
        assertTrue(first);
        verify(binService, times(1)).resetBinAfterCollection(binId);
        verify(routeCollectionBroadcaster, times(1)).broadcastBinStatusUpdate(sessionId, binId, "COLLECTED");

        boolean second = service.markBinCollected(sessionId, binId);
        assertFalse(second);
        verify(binService, times(1)).resetBinAfterCollection(binId);
        verify(routeCollectionBroadcaster, times(1)).broadcastBinStatusUpdate(sessionId, binId, "COLLECTED");
    }

    @Test
    public void markBinSkipped_and_pending_behaviour() {
        UUID sessionId = UUID.randomUUID();
        Long binId = 222L;
        RouteBinStop stop = new RouteBinStop(); stop.setId(5L); stop.setBinId(binId); stop.setStatus("PENDING");

        when(binStopRepository.findBySessionIdAndBinId(eq(sessionId), eq(binId))).thenReturn(Optional.of(stop));
        when(binStopRepository.markSkipped(eq(5L))).thenReturn(1).thenReturn(0);
        when(binStopRepository.markPending(eq(5L))).thenReturn(1).thenReturn(0);

        assertTrue(service.markBinSkipped(sessionId, binId));
        assertFalse(service.markBinSkipped(sessionId, binId));

        assertTrue(service.markBinPending(sessionId, binId));
        assertFalse(service.markBinPending(sessionId, binId));
    }

    @Test
    public void persist_saves_routes_and_updates_assignments() {
        RouteAssignmentRequestDTO req = new RouteAssignmentRequestDTO();
        req.setUserId(10L);
        req.setVehicleId(11L);
        req.setDriverId(12L);
        req.setSelectedBinIds(List.of(100L, 101L));

        RouteSessionSnapshotDTO snapshot = new RouteSessionSnapshotDTO(
        UUID.randomUUID().toString(),
        req.getUserId(),
        1L,
        "READY",
        "TEST",
        req.getSelectedBinIds(),
        Collections.emptyList(),
        Collections.emptyList(),
        Map.of("routes", Map.of(
                    "0", Map.of(
                            "capacity", 10,
                            "totalBins", 2,
                            "estimatedDurationSeconds", 123.0,
                            "binSequence", List.of(
                                    Map.of("stopOrder",1,"binId",100L,"lat",1.0,"lng",2.0,"durationFromPrevStopSeconds",10.0),
                                    Map.of("stopOrder",2,"binId",101L,"lat",1.1,"lng",2.1,"durationFromPrevStopSeconds",5.0)
                            )
                    )
            )),
        null   // <-- message, added to match the 10-arg constructor
);

        // routeAssignmentRepository empty
        when(routeAssignmentRepository.findBySessionId(any())).thenReturn(Optional.empty());

        // vehicle and driver exist
        com.garbo.core.entity.Vehicle vehicle = new com.garbo.core.entity.Vehicle(); vehicle.setId(11L); vehicle.setStatus("available");
        when(vehicleRepository.findById(11L)).thenReturn(Optional.of(vehicle));
        com.garbo.core.entity.BinCollector driver = new com.garbo.core.entity.BinCollector(); driver.setEmpId(12L);
        when(collectorRepository.findById(12L)).thenReturn(Optional.of(driver));

        when(vehicleRouteRepository.findBySessionIdWithStops(any())).thenReturn(Collections.emptyList());

        when(vehicleRouteRepository.save(any(RouteVehicleRoute.class))).thenAnswer(inv -> {
            RouteVehicleRoute vr = inv.getArgument(0);
            vr.setId(999L);
            return vr;
        });

        // execute
        service.persist(req, snapshot);

        // verify assignment saved and bins updated
        verify(routeAssignmentRepository, atLeastOnce()).save(any());
        verify(binRepository, times(2)).updateAssignedStatus(anyLong(), eq(true));
        verify(taskAlertBroadcaster, times(1)).notifyCollectorRouteAssigned(eq(10L), anyString(), eq(2), eq(11L));
        verify(notificationPublisher, times(1)).routeAssigned(eq(10L), anyString(), eq(2));
    }
}
