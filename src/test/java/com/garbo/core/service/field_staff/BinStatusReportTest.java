package com.garbo.core.service.field_staff;

import com.garbo.api.dto.BinReportRequest;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinReport;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinReportRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.CouncilBoundaryRepository;
import com.garbo.core.service.CouncilAccessService;
import com.garbo.core.service.UserTaskProgressService;
import com.garbo.core.service.notification.NotificationPublisher;
import com.garbo.infrastructure.websocket.TaskProgressBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class BinStatusReportTest {

    private BinRepository binRepository;
    private BinReportRepository binReportRepository;
    private FieldMentorRepository fieldMentorRepository;
    private CouncilAccessService councilAccessService;
    private CouncilBoundaryRepository councilBoundaryRepository;
    private UserTaskProgressService userTaskProgressService;
    private ApplicationEventPublisher eventPublisher;
    private NotificationPublisher notificationPublisher;
    private TaskProgressBroadcaster taskProgressBroadcaster;
    private BinService binService;

    @BeforeEach
    void setUp() {
        binRepository = Mockito.mock(BinRepository.class);
        binReportRepository = Mockito.mock(BinReportRepository.class);
        fieldMentorRepository = Mockito.mock(FieldMentorRepository.class);
        councilAccessService = Mockito.mock(CouncilAccessService.class);
        councilBoundaryRepository = Mockito.mock(CouncilBoundaryRepository.class);
        userTaskProgressService = Mockito.mock(UserTaskProgressService.class);
        eventPublisher = Mockito.mock(ApplicationEventPublisher.class);
        notificationPublisher = Mockito.mock(NotificationPublisher.class);
        taskProgressBroadcaster = Mockito.mock(TaskProgressBroadcaster.class);

        binService = new BinService(
                binRepository,
                binReportRepository,
                fieldMentorRepository,
                councilAccessService,
                councilBoundaryRepository,
                userTaskProgressService
        );

        // Inject fields using reflection or Mockito annotations if needed,
        // or just let Spring Autowire them in Spring Boot tests, or mock them:
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "binRepository", binRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "eventPublisher", eventPublisher);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "notificationPublisher", notificationPublisher);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "taskProgressBroadcaster", taskProgressBroadcaster);
        org.springframework.test.util.ReflectionTestUtils.setField(binService, "userTaskProgressService", userTaskProgressService);
    }

    @Test
    void reportBinStatus_noDiscrepancy_savesReportAndUpdatesBin() {
        Bin bin = new Bin();
        bin.setId(10L);
        bin.setStatus("empty");
        bin.setFillLevel(0);

        BinReportRequest request = new BinReportRequest();
        request.setStatus("empty");
        request.setFillLevel(0);
        request.setNotes("Bin is clean");

        FieldMentor mentor = new FieldMentor();
        mentor.setEmpId(200L);
        mentor.setEmpName("Mentor Alice");

        when(binRepository.findByNumericId(10L)).thenReturn(Optional.of(bin));
        when(fieldMentorRepository.findById(200L)).thenReturn(Optional.of(mentor));
        when(binRepository.updateStatusForReport(eq(10L), eq("empty"), anyInt())).thenReturn(1);

        BinReport mockSavedReport = new BinReport();
        mockSavedReport.setId(999L);
        mockSavedReport.setBin(bin);
        mockSavedReport.setReporter(mentor);
        mockSavedReport.setDiscrepancy(false);
        when(binReportRepository.save(any(BinReport.class))).thenReturn(mockSavedReport);
        when(userTaskProgressService.incrementFieldMentorReportTasks(anyLong(), anyLong())).thenReturn(java.util.Collections.emptyList());

        BinService.BinStatusReportResult result = binService.reportBinStatus(10L, 200L, request);

        assertNotNull(result);
        assertEquals(999L, result.reportId());
        assertFalse(result.discrepancy());
        assertEquals("empty", result.bin().getStatus());
    }

    @Test
    void reportBinStatus_withDiscrepancy_detectsAndFlagsDiscrepancy() {
        Bin bin = new Bin();
        bin.setId(10L);
        bin.setStatus("empty");
        bin.setFillLevel(0);

        BinReportRequest request = new BinReportRequest();
        request.setStatus("full"); // Change from empty to full -> discrepancy!
        request.setFillLevel(100);

        FieldMentor mentor = new FieldMentor();
        mentor.setEmpId(200L);
        mentor.setEmpName("Mentor Alice");

        when(binRepository.findByNumericId(10L)).thenReturn(Optional.of(bin));
        when(fieldMentorRepository.findById(200L)).thenReturn(Optional.of(mentor));
        when(binRepository.updateStatusForReport(eq(10L), eq("full"), anyInt())).thenReturn(1);

        BinReport mockSavedReport = new BinReport();
        mockSavedReport.setId(999L);
        mockSavedReport.setBin(bin);
        mockSavedReport.setReporter(mentor);
        mockSavedReport.setDiscrepancy(true);
        mockSavedReport.setPreviousStatus("empty");
        when(binReportRepository.save(any(BinReport.class))).thenReturn(mockSavedReport);
        when(userTaskProgressService.incrementFieldMentorReportTasks(anyLong(), anyLong())).thenReturn(java.util.Collections.emptyList());

        BinService.BinStatusReportResult result = binService.reportBinStatus(10L, 200L, request);

        assertNotNull(result);
        assertTrue(result.discrepancy());
        assertEquals("empty", result.previousStatus());
    }
}
