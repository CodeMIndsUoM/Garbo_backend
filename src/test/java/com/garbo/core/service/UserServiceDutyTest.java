package com.garbo.core.service;

import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.User;
import com.garbo.core.repository.AdminNewRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.email.EmailService;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class UserServiceDutyTest {

    private UserRepository userRepository;
    private AdminNewRepository adminNewRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private CloudinaryUploadService cloudinaryUploadService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        adminNewRepository = Mockito.mock(AdminNewRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        emailService = Mockito.mock(EmailService.class);
        cloudinaryUploadService = Mockito.mock(CloudinaryUploadService.class);

        userService = new UserService(
                userRepository,
                adminNewRepository,
                passwordEncoder,
                emailService,
                cloudinaryUploadService
        );
    }

    @Test
    void changePassword_firstLoginForCollector_setsOnDutyTrue() {
        BinCollector collector = new BinCollector();
        collector.setEmpId(10L);
        collector.setEmail("collector@garbo.local");
        collector.setPassword("temp-plain");
        collector.setMustChangePassword(true);
        collector.setOnDuty(false);

        when(userRepository.findFirstByEmailIgnoreCase("collector@garbo.local")).thenReturn(Optional.of(collector));
        when(passwordEncoder.matches("temp-plain", "temp-plain")).thenReturn(true);
        when(passwordEncoder.encode("new-plain")).thenReturn("hashed-new");

        userService.changePassword("collector@garbo.local", "temp-plain", "new-plain");

        assertEquals("hashed-new", collector.getPassword());
        assertFalse(collector.isMustChangePassword());
        assertTrue(collector.isOnDuty());
    }

    @Test
    void changePassword_firstLoginForMentor_setsOnDutyTrue() {
        FieldMentor mentor = new FieldMentor();
        mentor.setEmpId(20L);
        mentor.setEmail("mentor@garbo.local");
        mentor.setPassword("temp-plain");
        mentor.setMustChangePassword(true);
        mentor.setOnDuty(false);

        when(userRepository.findFirstByEmailIgnoreCase("mentor@garbo.local")).thenReturn(Optional.of(mentor));
        when(passwordEncoder.matches("temp-plain", "temp-plain")).thenReturn(true);
        when(passwordEncoder.encode("new-plain")).thenReturn("hashed-new");

        userService.changePassword("mentor@garbo.local", "temp-plain", "new-plain");

        assertEquals("hashed-new", mentor.getPassword());
        assertFalse(mentor.isMustChangePassword());
        assertTrue(mentor.isOnDuty());
    }
}
