package com.garbo.core.service;

import com.garbo.core.entity.Complaint;
import com.garbo.core.entity.User;
import com.garbo.core.repository.ComplaintRepository;
import com.garbo.core.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CouncilAccessService councilAccessService;

    @InjectMocks
    private ComplaintService complaintService;

    @Test
    void getAllComplaintsForRequester_returnsCouncilScopedResults() {
        Complaint complaint = new Complaint();
        complaint.setTitle("Overflowing bin");

        when(councilAccessService.isSuperAdmin("admin@garbo.com")).thenReturn(false);
        when(councilAccessService.resolveCouncilForEmail("admin@garbo.com"))
                .thenReturn(Optional.of("Colombo"));
        when(complaintRepository.findByCitizenCouncil("Colombo"))
                .thenReturn(List.of(complaint));

        List<Complaint> results = complaintService.getAllComplaintsForRequester("admin@garbo.com");

        assertEquals(1, results.size());
        assertEquals("Overflowing bin", results.get(0).getTitle());
        verify(complaintRepository).findByCitizenCouncil("Colombo");
    }

    @Test
    void getAllComplaintsForRequester_returnsEmptyWhenCouncilMissing() {
        when(councilAccessService.isSuperAdmin("admin@garbo.com")).thenReturn(false);
        when(councilAccessService.resolveCouncilForEmail("admin@garbo.com"))
                .thenReturn(Optional.empty());

        List<Complaint> results = complaintService.getAllComplaintsForRequester("admin@garbo.com");

        assertTrue(results.isEmpty());
    }

    @Test
    void getComplaintById_allowsCitizenOwnComplaint() {
        User citizen = new User();
        citizen.setEmpId(101L);
        citizen.setRole("CITIZEN");
        when(userRepository.findByEmail("citizen@garbo.com")).thenReturn(Optional.of(citizen));

        Complaint complaint = new Complaint();
        complaint.setId(1L);
        complaint.setCitizen(citizen);

        when(councilAccessService.isSuperAdmin("citizen@garbo.com")).thenReturn(false);
        when(councilAccessService.resolveCouncilForEmail("citizen@garbo.com"))
                .thenReturn(Optional.of("Colombo"));
        when(complaintRepository.findById(1L)).thenReturn(Optional.of(complaint));

        Complaint result = complaintService.getComplaintById(1L, "citizen@garbo.com");
        assertEquals(1L, result.getId());
    }
}
