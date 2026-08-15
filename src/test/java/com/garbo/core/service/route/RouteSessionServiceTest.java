package com.garbo.core.service.route;

import com.garbo.api.dto.RouteAssignmentRequestDTO;
import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.ComplaintRepository;
import com.garbo.core.service.ComplaintService;
import com.garbo.domain.ORToolsWrapper;
import com.garbo.domain.OSRMClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
public class RouteSessionServiceTest {

    @Mock
    BinRepository binRepository;

    @Mock
    ComplaintRepository complaintRepository;

    @Mock
    ComplaintService complaintService;

    @Mock
    SimpMessagingTemplate messagingTemplate;

    @Mock
    RouteAssignmentService routeAssignmentService;

    private RouteSessionService service;

    @BeforeEach
    public void setup() {
        service = new RouteSessionService(binRepository, complaintRepository, complaintService, messagingTemplate, routeAssignmentService);
    }

    @Test
    public void validateRequest_invalidInput_throws() {
        RouteSessionCreateRequestDTO req = new RouteSessionCreateRequestDTO();
        req.setUserId(null);
        assertThrows(IllegalArgumentException.class, () -> service.createSession(req));
    }

    @Test
    public void optimizeAndBroadcast_success_returnsReadyAndPublishes() throws Exception {
        RouteAssignmentRequestDTO req = new RouteAssignmentRequestDTO();
        req.setUserId(42L);
        req.setDepotLat(1.0);
        req.setDepotLng(2.0);
        req.setVehicleCount(1);
        req.setVehicleCapacities(new int[]{10});
        req.setSelectedBinIds(List.of(1L,2L));
        // make it a valid assignment so persist() path is exercised
        req.setVehicleId(100L);
        req.setDriverId(200L);

        Bin b1 = new Bin(10.0,20.0,100,"full"); b1.setId(1L);
        Bin b2 = new Bin(11.0,21.0,100,"full"); b2.setId(2L);
        when(binRepository.findAllByIdWithCast(anyList())).thenReturn(List.of(b1,b2));

        // Mock static OSRMClient.getDurationMatrix
        double[][] matrix = new double[][]{
                {0,10,20},
                {10,0,5},
                {20,5,0}
        };

        try (var osrm = mockStatic(OSRMClient.class)) {
            osrm.when(() -> OSRMClient.getDurationMatrix(any(double[][].class))).thenReturn(matrix);

            // Mock ORToolsWrapper construction and solve
            Map<Integer, List<Long>> routes = new HashMap<>();
            routes.put(0, List.of(1L,2L));
            try (var construction = mockConstruction(ORToolsWrapper.class,
                    (mock, context) -> when(mock.solve(any(double[][].class), anyList(), anyInt(), any(int[].class))).thenReturn(routes))) {

                RouteSessionSnapshotDTO snap = service.optimizeAndBroadcast(req);

                assertNotNull(snap);
                assertEquals("READY", snap.getStatus());
                // messagingTemplate should have been used to publish at least once (processing + ready)
                verify(messagingTemplate, atLeastOnce()).convertAndSend(startsWith("/topic/"), any(Object.class));

                // persist should be attempted for assignment requests with valid team
                verify(routeAssignmentService, timeout(1000).atLeastOnce()).persist(any(), any());
            }
        }
    }

    @Test
    public void optimizeAndBroadcast_solverThrows_returnsErrorSnapshot() throws Exception {
        RouteSessionCreateRequestDTO req = new RouteSessionCreateRequestDTO();
        req.setUserId(7L);
        req.setDepotLat(1.0);
        req.setDepotLng(2.0);
        req.setVehicleCount(1);
        req.setVehicleCapacities(new int[]{10});
        req.setSelectedBinIds(List.of(1L));

        Bin b1 = new Bin(10.0,20.0,100,"full"); b1.setId(1L);
        when(binRepository.findAllByIdWithCast(anyList())).thenReturn(List.of(b1));

        double[][] matrix = new double[][]{{0,10},{10,0}};

        try (var osrm = mockStatic(OSRMClient.class)) {
            osrm.when(() -> OSRMClient.getDurationMatrix(any(double[][].class))).thenReturn(matrix);

            try (var construction = mockConstruction(ORToolsWrapper.class,
                    (mock, context) -> when(mock.solve(any(double[][].class), anyList(), anyInt(), any(int[].class))).thenThrow(new RuntimeException("distance failure")))) {

                RouteSessionSnapshotDTO snap = service.optimizeAndBroadcast(req);
                assertNotNull(snap);
                assertEquals("ERROR", snap.getStatus());
                assertTrue(snap.getMessage().toLowerCase().contains("distance failure"));
            }
        }
    }

    @Test
    public void createSession_publishesProcessingSnapshot() {
        RouteSessionCreateRequestDTO req = new RouteSessionCreateRequestDTO();
        req.setUserId(55L);
        req.setDepotLat(0.1);
        req.setDepotLng(0.2);
        req.setSelectedBinIds(List.of());

        RouteSessionSnapshotDTO snap = service.createSession(req);
        assertNotNull(snap);
        assertEquals("PROCESSING", snap.getStatus());
        verify(messagingTemplate, atLeastOnce()).convertAndSend(startsWith("/topic/"), any(Object.class));
    }
}
