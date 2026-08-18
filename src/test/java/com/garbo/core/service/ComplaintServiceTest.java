package com.garbo.core.service;

import com.garbo.api.dto.ComplaintCreateRequest;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.User;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.ComplaintRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.service.notification.NotificationPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CitizenRepository citizenRepository;

    @Mock
    private CouncilAccessService councilAccessService;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private BinRepository binRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private User citizenUser;
    private Citizen citizenEntity;
    private User staffUser;
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        citizenUser = new User();
        citizenUser.setEmpId(101L);
        citizenUser.setEmail("citizen@example.com");
        citizenUser.setRole("CITIZEN");

        citizenEntity = new Citizen();
        citizenEntity.setEmpId(101L);
        citizenEntity.setCouncil("Moratuwa");

        staffUser = new User();
        staffUser.setEmpId(202L);
        staffUser.setEmail("staff@example.com");
        staffUser.setRole("FIELD_STAFF");

        complaint = new Complaint();
        complaint.setId(10L);
        complaint.setCitizenId(101L);
        complaint.setAssignedPersonnelId(202L);
        complaint.setCouncil("Moratuwa");
        complaint.setTitle("Overflowing Bin in Main St");
        complaint.setStatus("IN_PROGRESS");
    }

    @Test
    void createComplaint_Success() {
        when(userRepository.findFirstByEmailIgnoreCase("citizen@example.com")).thenReturn(Optional.of(citizenUser));
        when(citizenRepository.findFirstByEmailIgnoreCase("citizen@example.com")).thenReturn(Optional.of(citizenEntity));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(invocation -> {
            Complaint c = invocation.getArgument(0);
            c.setId(99L);
            return c;
        });

        ComplaintCreateRequest request = new ComplaintCreateRequest();
        request.setTitle("Overflowing Bin");
        request.setIssueType("Overflowing Bin");
        request.setUrgency("High");
        request.setLocation("6.9271,79.8612");
        request.setDescription("Needs urgent cleanup");

        Complaint created = complaintService.createComplaint(request, "citizen@example.com");

        assertNotNull(created);
        assertEquals("PENDING", created.getStatus());
        assertEquals("Moratuwa", created.getCouncil());
        assertEquals(101L, created.getCitizenId());
        verify(notificationPublisher, times(1)).complaintSubmitted(created);
    }

    @Test
    void getAssignedComplaints_ReturnsAssignedList() {
        when(userRepository.findFirstByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staffUser));
        when(complaintRepository.findByAssignedTo(staffUser)).thenReturn(List.of(complaint));

        List<Complaint> assigned = complaintService.getAssignedComplaints("staff@example.com");

        assertNotNull(assigned);
        assertEquals(1, assigned.size());
        assertEquals("Overflowing Bin in Main St", assigned.get(0).getTitle());
    }

    @Test
    void confirmComplaint_Approve_SetsStatusResolved() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(userRepository.findFirstByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staffUser));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        Complaint confirmed = complaintService.confirmComplaint(10L, true, "Verified on site and cleared", "https://photo.url", "staff@example.com");

        assertNotNull(confirmed);
        assertEquals("RESOLVED", confirmed.getStatus());
        assertTrue(confirmed.getIsConfirmedTrue());
        assertEquals("Verified on site and cleared", confirmed.getFieldStaffNote());
        assertEquals("https://photo.url", confirmed.getFieldStaffPhotoUrl());
        verify(notificationPublisher, times(1)).complaintStatusUpdated(confirmed);
    }

    @Test
    void confirmComplaint_Reject_SetsStatusRejectedByStaff() {
        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(userRepository.findFirstByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staffUser));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        Complaint confirmed = complaintService.confirmComplaint(10L, false, "False alarm, already empty", "https://photo.url", "staff@example.com");

        assertNotNull(confirmed);
        assertEquals("REJECTED_BY_STAFF", confirmed.getStatus());
        assertFalse(confirmed.getIsConfirmedTrue());
        assertEquals("False alarm, already empty", confirmed.getFieldStaffNote());
        verify(notificationPublisher, times(1)).complaintStatusUpdated(confirmed);
    }

    @Test
    void confirmComplaint_ThrowsAccessDenied_WhenNotAssignedStaff() {
        User otherStaff = new User();
        otherStaff.setEmpId(999L);
        otherStaff.setEmail("other@example.com");

        when(complaintRepository.findById(10L)).thenReturn(Optional.of(complaint));
        when(userRepository.findFirstByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(otherStaff));

        assertThrows(AccessDeniedException.class, () ->
                complaintService.confirmComplaint(10L, true, "note", null, "other@example.com")
        );
    }

    @Test
    void addToRoute_CreatesProxyBinAndSetsStatusAddedToRoute() {
        complaint.setLocation("6.9271,79.8612");
        when(complaintRepository.findAllById(List.of(10L))).thenReturn(List.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenAnswer(inv -> inv.getArgument(0));

        complaintService.addToRoute(List.of(10L));

        assertEquals("ADDED_TO_ROUTE", complaint.getStatus());
        verify(binRepository, times(1)).save(any());
    }
}
