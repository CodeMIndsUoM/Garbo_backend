package com.garbo.core.service;

import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.enums.RegistrationStatus;
import com.garbo.core.repository.CouncilRepository;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import com.garbo.core.service.notification.NotificationPublisher;
import com.garbo.core.service.third_party_collector.ThirdPartyCollectorRegistrationService;
import com.garbo.infrastructure.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ThirdPartyRegistrationFlowTest {

    private ThirdPartyCollectorRepository repository;
    private CouncilRepository councilRepository;
    private PasswordEncoder passwordEncoder;
    private EmailService emailService;
    private NotificationPublisher notificationPublisher;
    private ThirdPartyCollectorRegistrationService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ThirdPartyCollectorRepository.class);
        councilRepository = Mockito.mock(CouncilRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        emailService = Mockito.mock(EmailService.class);
        notificationPublisher = Mockito.mock(NotificationPublisher.class);

        service = new ThirdPartyCollectorRegistrationService(
                repository,
                councilRepository,
                passwordEncoder,
                emailService,
                notificationPublisher
        );
    }

    @Test
    void thirdParty_approvePendingCollector_approvesAndSendsCredentialsEmail() throws Exception {
        ThirdPartyCollector collector = new ThirdPartyCollector();
        collector.setEmpId(11L);
        collector.setEmpName("Collector X");
        collector.setEmail("collectorx@garbo.local");
        collector.setRegistrationStatus(RegistrationStatus.PENDING);
        collector.setPassword(null);

        when(repository.findById(11L)).thenReturn(Optional.of(collector));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp-password");
        when(repository.save(any(ThirdPartyCollector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ThirdPartyCollector saved = service.approve(11L);

        assertEquals(RegistrationStatus.APPROVED, saved.getRegistrationStatus());
        verify(emailService).sendAdminCredentials(eq("collectorx@garbo.local"), anyString());
        verify(notificationPublisher).registrationApproved(saved);
    }

    @Test
    void thirdParty_rejectPendingCollector_rejectsAndSendsRejectionEmail() {
        ThirdPartyCollector collector = new ThirdPartyCollector();
        collector.setEmpId(12L);
        collector.setEmpName("Collector Y");
        collector.setEmail("collectory@garbo.local");
        collector.setRegistrationStatus(RegistrationStatus.PENDING);

        when(repository.findById(12L)).thenReturn(Optional.of(collector));
        when(repository.save(any(ThirdPartyCollector.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ThirdPartyCollector saved = service.reject(12L, "Incomplete documents");

        assertEquals(RegistrationStatus.REJECTED, saved.getRegistrationStatus());
        verify(emailService).sendRegistrationRejected("collectory@garbo.local", "Collector Y", "Incomplete documents");
        verify(notificationPublisher).registrationRejected(saved);
    }

    @Test
    void thirdParty_approveNotPending_throwsStateError() {
        ThirdPartyCollector collector = new ThirdPartyCollector();
        collector.setEmpId(13L);
        collector.setRegistrationStatus(RegistrationStatus.APPROVED);

        when(repository.findById(13L)).thenReturn(Optional.of(collector));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.approve(13L));
        assertEquals("Third-party collector is not in PENDING status. Current: APPROVED", ex.getMessage());
    }

    @Test
    void thirdParty_setPassword_notApproved_throwsStateError() {
        ThirdPartyCollector collector = new ThirdPartyCollector();
        collector.setEmpId(14L);
        collector.setEmail("collectorz@garbo.local");
        collector.setRegistrationStatus(RegistrationStatus.PENDING);

        when(repository.findById(14L)).thenReturn(Optional.of(collector));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.setPassword(14L, "collectorz@garbo.local", "Password123!"));

        assertEquals("Third-party collector registration is not yet approved", ex.getMessage());
    }
}
