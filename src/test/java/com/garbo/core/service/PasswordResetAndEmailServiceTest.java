package com.garbo.core.service;

import com.garbo.core.entity.User;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.email.EmailService;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetAndEmailServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        emailService = Mockito.mock(EmailService.class);
        userService = new UserService(
                userRepository,
                Mockito.mock(com.garbo.core.repository.AdminNewRepository.class),
                passwordEncoder,
                emailService,
                Mockito.mock(CloudinaryUploadService.class)
        );
    }

    @Test
    void passwordReset_requestReset_existingUser_createsToken() {
        User user = new User();
        user.setEmpId(20L);
        user.setEmail("reset@garbo.local");
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);

        when(userRepository.findFirstByEmailIgnoreCase("reset@garbo.local")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.requestPasswordReset("reset@garbo.local");

        assertNotNull(user.getPasswordResetToken());
        assertNotNull(user.getPasswordResetExpiresAt());
        assertEquals(true, user.getPasswordResetExpiresAt().isAfter(LocalDateTime.now()));
        verify(emailService).sendPasswordResetEmail("reset@garbo.local", user.getPasswordResetToken());
    }

    @Test
    void passwordReset_validToken_updatesPassword() {
        User user = new User();
        user.setEmpId(21L);
        user.setEmail("validreset@garbo.local");
        user.setPassword("old-password");
        user.setPasswordResetToken("abc123");
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(30));
        user.setMustChangePassword(true);

        when(userRepository.findByPasswordResetToken("abc123")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass123!")).thenReturn("hashed-new");
        when(userRepository.save(user)).thenReturn(user);

        userService.resetPasswordWithToken("abc123", "NewPass123!");

        assertEquals("hashed-new", user.getPassword());
        assertNull(user.getPasswordResetToken());
        assertNull(user.getPasswordResetExpiresAt());
        assertEquals(false, user.isMustChangePassword());
    }

    @Test
    void passwordReset_expiredToken_returnsError() {
        User user = new User();
        user.setEmpId(22L);
        user.setEmail("expired@garbo.local");
        user.setPasswordResetToken("expired-token");
        user.setPasswordResetExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByPasswordResetToken("expired-token")).thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.resetPasswordWithToken("expired-token", "NewPass123!"));

        assertEquals("Reset token has expired", ex.getMessage());
    }

    @Test
    void requestPasswordReset_sendsEmailToMatchedUser() {
        User user = new User();
        user.setEmpId(23L);
        user.setEmail("emailtest@garbo.local");

        when(userRepository.findFirstByEmailIgnoreCase("emailtest@garbo.local")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.requestPasswordReset("emailtest@garbo.local");

        verify(emailService).sendPasswordResetEmail(eq("emailtest@garbo.local"), anyString());
    }
}
