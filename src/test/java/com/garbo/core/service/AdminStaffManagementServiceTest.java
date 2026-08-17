package com.garbo.core.service;

import com.garbo.api.dto.staff.StaffCreateRequest;
import com.garbo.api.dto.staff.StaffUpdateRequest;
import com.garbo.api.dto.staff.UserSummaryDto;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AdminStaffManagementServiceTest {

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
    void adminStaff_createFieldMentor_validRequest_returnsCreatedStaff() {
        StaffCreateRequest request = new StaffCreateRequest();
        request.setEmail("mentor@garbo.local");
        request.setFullName("Mentor One");
        request.setContactNumber("0771111111");

        when(userRepository.findFirstByEmailIgnoreCase("mentor@garbo.local")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashed-password");

        FieldMentor saved = new FieldMentor();
        saved.setEmpId(7L);
        saved.setEmpName("Mentor One");
        saved.setEmail("mentor@garbo.local");
        saved.setRole("FIELD_MENTOR");
        saved.setAssignedCouncil("KMC");
        saved.setMustChangePassword(true);

        when(fieldMentorRepository.save(any(FieldMentor.class))).thenReturn(saved);

        Optional<UserSummaryDto> result = adminStaffService.createFieldMentor(request, "KMC");

        assertTrue(result.isPresent());
        assertEquals("Mentor One", result.get().getEmpName());
        assertEquals("FIELD_MENTOR", result.get().getRole());
    }

    @Test
    void adminStaff_updateFieldMentor_validRequest_updatesRecord() {
        FieldMentor mentor = new FieldMentor();
        mentor.setEmpId(8L);
        mentor.setEmpName("Old Name");
        mentor.setEmail("mentor@garbo.local");
        mentor.setPhone("0770000000");
        mentor.setAssignedCouncil("KMC");
        mentor.setRole("FIELD_MENTOR");

        StaffUpdateRequest request = new StaffUpdateRequest();
        request.setFullName("Updated Name");
        request.setContactNumber("0779999999");

        when(fieldMentorRepository.findById(8L)).thenReturn(Optional.of(mentor));
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Optional<UserSummaryDto> result = adminStaffService.updateFieldMentor(8L, request, "KMC");

        assertTrue(result.isPresent());
        assertEquals("Updated Name", result.get().getEmpName());
    }

    @Test
    void adminStaff_hideStaff_forbiddenOutsideCouncil_returns403() {
        FieldMentor mentor = new FieldMentor();
        mentor.setEmpId(9L);
        mentor.setAssignedCouncil("KMC");
        mentor.setRole("FIELD_MENTOR");

        when(userRepository.findById(9L)).thenReturn(Optional.of(mentor));

        String result = adminStaffService.hideInternalUser(9L, "Gampaha");

        assertEquals("FORBIDDEN", result);
    }

    @Test
    void adminStaff_deleteStaff_existingUser_deletesRecord() {
        FieldMentor mentor = new FieldMentor();
        mentor.setEmpId(10L);
        mentor.setAssignedCouncil("KMC");
        mentor.setRole("FIELD_MENTOR");

        when(userRepository.findById(10L)).thenReturn(Optional.of(mentor));

        String result = adminStaffService.deleteInternalUser(10L, "KMC");

        assertEquals("DELETED", result);
    }
}
