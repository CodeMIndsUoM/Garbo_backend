package com.garbo.core.service.field_staff;

import com.garbo.api.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinReport;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinReportRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.service.CouncilAccessService;
import com.garbo.core.service.UserTaskProgressService;
import com.garbo.core.service.notification.NotificationPublisher;
import com.garbo.infrastructure.websocket.TaskProgressBroadcaster;
import com.garbo.infrastructure.websocket.TaskAlertBroadcaster;
import com.garbo.infrastructure.websocket.RouteCollectionBroadcaster;
import com.garbo.core.repository.RouteBinStopRepository;
import com.garbo.core.service.zone.ZoneClusteringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BinServiceTest {

    @Mock
    private BinRepository binRepository;

    @Mock
    private BinReportRepository binReportRepository;

    @Mock
    private FieldMentorRepository fieldMentorRepository;

    @Mock
    private CouncilAccessService councilAccessService;

    @Mock
    private UserTaskProgressService userTaskProgressService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private TaskProgressBroadcaster taskProgressBroadcaster;

    @Mock
    private TaskAlertBroadcaster taskAlertBroadcaster;

    @Mock
    private RouteCollectionBroadcaster routeCollectionBroadcaster;

    @Mock
    private RouteBinStopRepository routeBinStopRepository;

    @Mock
    private ZoneClusteringService zoneClusteringService;

    @InjectMocks
    private BinService binService;

    private Bin testBin;
    private FieldMentor testMentor;

    @BeforeEach
    void setUp() {
        testBin = new Bin();
        testBin.setId(1L);
        testBin.setStatus("empty");
        testBin.setFillLevel(0);

        testMentor = new FieldMentor();
        testMentor.setEmpId(2L);
        testMentor.setEmpName("Mentor A");

        org.springframework.test.util.ReflectionTestUtils.setField(binService, "eventPublisher", eventPublisher);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "notificationPublisher", notificationPublisher);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "taskProgressBroadcaster", taskProgressBroadcaster);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "taskAlertBroadcaster", taskAlertBroadcaster);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "routeCollectionBroadcaster", routeCollectionBroadcaster);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "routeBinStopRepository", routeBinStopRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "zoneClusteringService", zoneClusteringService);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "binRepository", binRepository);
    }

    @Test
    void reportBinStatus_success() {
        BinReportRequest request = new BinReportRequest();
        request.setStatus("full");
        request.setFillLevel(100);
        request.setLatitude(1.0);
        request.setLongitude(2.0);

        when(binRepository.findByNumericId(1L)).thenReturn(Optional.of(testBin));
        when(fieldMentorRepository.findById(2L)).thenReturn(Optional.of(testMentor));

        BinReport mockReport = new BinReport();
        mockReport.setId(10L);
        mockReport.setDiscrepancy(true);
        mockReport.setPreviousStatus("empty");
        when(binReportRepository.save(any(BinReport.class))).thenReturn(mockReport);
        when(binRepository.updateStatusForReport(eq(1L), eq("full"), eq(100))).thenReturn(1);

        BinService.BinStatusReportResult result = binService.reportBinStatus(1L, 2L, request);

        assertEquals(10L, result.reportId());
        assertEquals("full", result.bin().getStatus());
        assertEquals(100, result.bin().getFillLevel());
        verify(binReportRepository, times(1)).save(any(BinReport.class));
        verify(binRepository, times(1)).updateStatusForReport(1L, "full", 100);
    }

    @Test
    void getAssignedBins_success() {
        when(binRepository.findByAssignedToEmpId(2L)).thenReturn(List.of(testBin));
        
        List<Bin> result = binService.getAssignedBins(2L);
        
        assertEquals(1, result.size());
        verify(binRepository, times(1)).findByAssignedToEmpId(2L);
    }

    @Test
    void undoBinReport_success() {
        when(fieldMentorRepository.findById(2L)).thenReturn(Optional.of(testMentor));
        when(binRepository.resetStatusForUndo(1L)).thenReturn(1);
        
        Bin result = binService.undoBinReport(1L, 2L);
        
        assertEquals("notChecked", result.getStatus());
        assertEquals(0, result.getFillLevel());
    }

    @Test
    void createBin_success() {
        Bin savedBin = new Bin();
        savedBin.setId(1L);
        when(binRepository.save(any(Bin.class))).thenReturn(savedBin);
        
        Bin result = binService.createBin(testBin);
        
        assertEquals(1L, result.getId());
    }

    @Test
    void getBins_byCouncil() {
        when(binRepository.findByCouncilIgnoreCase("CouncilA")).thenReturn(List.of(testBin));
        
        List<Bin> result = binService.getBins("CouncilA");
        
        assertEquals(1, result.size());
    }

    @Test
    void deleteBinForCurrentUser_success() {
        // Mock security context for current user
        org.springframework.security.core.Authentication authentication = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        when(binRepository.findById(1L)).thenReturn(Optional.of(testBin));
        when(councilAccessService.isSuperAdmin("test@example.com")).thenReturn(true);
        when(councilAccessService.resolveCouncilForEmail("test@example.com")).thenReturn(Optional.of("CouncilA"));

        binService.deleteBinForCurrentUser(1L);

        verify(binReportRepository, times(1)).deleteByBinId(1L);
        verify(binRepository, times(1)).deleteByIdNative(1L);
        
        // Clear security context
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updatePriorityForCurrentUser_success() {
        org.springframework.security.core.Authentication authentication = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        when(binRepository.findById(1L)).thenReturn(Optional.of(testBin));
        when(councilAccessService.isSuperAdmin("test@example.com")).thenReturn(true);
        when(councilAccessService.resolveCouncilForEmail("test@example.com")).thenReturn(Optional.of("CouncilA"));

        Bin result = binService.updatePriorityForCurrentUser(1L, "high");

        assertEquals("high", result.getPriority());
        verify(binRepository, times(1)).updatePriorityNative(1L, "high");
        
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void updateZoneForCurrentUser_success() {
        org.springframework.security.core.Authentication authentication = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        when(binRepository.findById(1L)).thenReturn(Optional.of(testBin));
        when(councilAccessService.isSuperAdmin("test@example.com")).thenReturn(true);
        when(councilAccessService.resolveCouncilForEmail("test@example.com")).thenReturn(Optional.of("CouncilA"));

        Bin result = binService.updateZoneForCurrentUser(1L, "Zone 2");

        assertEquals("Zone 2", result.getZone());
        verify(binRepository, times(1)).updateZoneNative(1L, "Zone 2");
        
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void assignMentorToBin_success() {
        org.springframework.security.core.Authentication authentication = mock(org.springframework.security.core.Authentication.class);
        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("test@example.com");
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        when(binRepository.findById(1L)).thenReturn(Optional.of(testBin));
        when(councilAccessService.isSuperAdmin("test@example.com")).thenReturn(true);
        when(councilAccessService.resolveCouncilForEmail("test@example.com")).thenReturn(Optional.of("CouncilA"));

        when(fieldMentorRepository.findById(2L)).thenReturn(Optional.of(testMentor));
        when(binRepository.save(any(Bin.class))).thenReturn(testBin);

        Bin result = binService.assignMentorToBin(1L, 2L);

        assertEquals(testMentor, result.getAssignedTo());
        verify(binRepository, times(1)).save(testBin);
        
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
