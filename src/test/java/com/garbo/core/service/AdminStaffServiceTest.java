package com.garbo.core.service;

import com.garbo.api.dto.staff.StaffCreateRequest;
import com.garbo.api.dto.staff.StaffListDto;
import com.garbo.api.dto.staff.UserSummaryDto;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.User;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AdminStaffServiceTest {

    private FieldMentorRepository fieldMentorRepository;
    private BinCollectorRepository binCollectorRepository;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private AdminStaffService adminStaffService;

    @BeforeEach
    void setUp() {
        fieldMentorRepository = Mockito.mock(FieldMentorRepository.class);
        binCollectorRepository = Mockito.mock(BinCollectorRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        emailService = Mockito.mock(EmailService.class);

        adminStaffService = new AdminStaffService(
                fieldMentorRepository,
                binCollectorRepository,
                userRepository,
                passwordEncoder,
                emailService
        );
        adminStaffService.systemIncidentService = Mockito.mock(com.garbo.core.service.security.SystemIncidentService.class);
    }

    @Test
    void createBinCollector_success_returnsSummary() {
        StaffCreateRequest req = new StaffCreateRequest();
        req.setEmail("collector@garbo.local");
        req.setFullName("Collector Bob");
        req.setContactNumber("0771234567");

        when(userRepository.findFirstByEmailIgnoreCase("collector@garbo.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed-pwd");

        BinCollector saved = new BinCollector();
        saved.setEmpId(10L);
        saved.setEmpName("Collector Bob");
        saved.setEmail("collector@garbo.local");
        saved.setRole("BIN_COLLECTOR");
        saved.setOnDuty(false);

        when(binCollectorRepository.save(any(BinCollector.class))).thenReturn(saved);

        Optional<UserSummaryDto> res = adminStaffService.createBinCollector(req, "Colombo");

        assertTrue(res.isPresent());
        assertEquals("Collector Bob", res.get().getEmpName());
        assertEquals("BIN_COLLECTOR", res.get().getRole());
        assertFalse(saved.isOnDuty());
    }

    @Test
    void listStaffForCurrentAdmin_returnsList() {
        FieldMentor m1 = new FieldMentor();
        m1.setEmpId(1L);
        m1.setEmpName("Mentor Alice");
        m1.setEmail("alice@garbo.local");
        m1.setRole("FIELD_MENTOR");
        m1.setOnDuty(true);

        BinCollector c1 = new BinCollector();
        c1.setEmpId(2L);
        c1.setEmpName("Collector Bob");
        c1.setEmail("bob@garbo.local");
        c1.setRole("BIN_COLLECTOR");
        c1.setOnDuty(false);

        when(fieldMentorRepository.findByAssignedCouncil("Colombo")).thenReturn(List.of(m1));
        when(binCollectorRepository.findByAssignedCouncil("Colombo")).thenReturn(List.of(c1));

        List<StaffListDto> res = adminStaffService.listStaffForCurrentAdmin("Colombo");

        assertEquals(2, res.size());
        assertEquals("Mentor Alice", res.get(0).getEmpName());
        assertTrue(Boolean.TRUE.equals(res.get(0).getOnDuty()));
        assertEquals("Collector Bob", res.get(1).getEmpName());
        assertFalse(Boolean.TRUE.equals(res.get(1).getOnDuty()));
    }
}
