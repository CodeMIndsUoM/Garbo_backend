package com.garbo.core.service.third_party_collector;

import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.entity.Council;
import com.garbo.core.enums.RegistrationStatus;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import com.garbo.core.repository.CouncilRepository;
import com.garbo.common.logging.AdminCreationLogger;
import com.garbo.infrastructure.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ThirdPartyCollectorRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyCollectorRegistrationService.class);
    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*()-_";

    private final ThirdPartyCollectorRepository repository;
    private final CouncilRepository councilRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ThirdPartyCollectorRegistrationService(
            ThirdPartyCollectorRepository repository,
            CouncilRepository councilRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.repository = repository;
        this.councilRepository = councilRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public ThirdPartyCollector register(
            String empName,
            String email,
            String phone,
            String nic,
            String dateOfBirth,
            String company,
            String contractId,
            String contractStart,
            String contractEnd,
            String defaultAddress,
            String nicPhotoUrl,
            String nicPhotoBackUrl,
            List<String> assignedCouncils) {

        Optional<ThirdPartyCollector> existing = repository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A third-party collector with this email already exists");
        }

        ThirdPartyCollector collector = new ThirdPartyCollector();
        collector.setEmpName(empName);
        collector.setEmail(email);
        collector.setPhone(phone);
        collector.setNIC(nic);
        collector.setCompany(company);
        collector.setDefaultAddress(defaultAddress);
        collector.setNicPhotoUrl(nicPhotoUrl);
        collector.setNicPhotoBackUrl(nicPhotoBackUrl);
        if (assignedCouncils != null && !assignedCouncils.isEmpty()) {
            collector.setAssignedCouncils(String.join(",", assignedCouncils));
        }
        collector.setRole("THIRD_PARTY_COLLECTOR");
        collector.setRegistrationStatus(RegistrationStatus.PENDING);
        collector.setCompletedRequests(0);
        collector.setCreatedAt(LocalDateTime.now());

        if (dateOfBirth != null && !dateOfBirth.isBlank()) {
            collector.setDateOfBirth(LocalDate.parse(dateOfBirth));
        }
        if (contractId != null && !contractId.isBlank()) {
            collector.setContractId(contractId);
        }
        if (contractStart != null && !contractStart.isBlank()) {
            collector.setContractStart(LocalDate.parse(contractStart));
        }
        if (contractEnd != null && !contractEnd.isBlank()) {
            collector.setContractEnd(LocalDate.parse(contractEnd));
        }

        ThirdPartyCollector saved = repository.save(collector);
        log.info("Third-party collector registration submitted: empId={}, email={}", saved.getEmpId(), saved.getEmail());
        return saved;
    }

    public ThirdPartyCollector approve(Long empId) {
        ThirdPartyCollector collector = repository.findById(empId)
                .orElseThrow(() -> new IllegalArgumentException("Third-party collector not found with id: " + empId));

        if (collector.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException(
                    "Third-party collector is not in PENDING status. Current: " + collector.getRegistrationStatus());
        }

        collector.setRegistrationStatus(RegistrationStatus.APPROVED);

        boolean firstCredentials = collector.getPassword() == null || collector.getPassword().isBlank();
        String tempPassword = null;
        if (firstCredentials) {
            tempPassword = generateTemporaryPassword(12);
            collector.setPassword(passwordEncoder.encode(tempPassword));
            collector.setMustChangePassword(true);
        }

        ThirdPartyCollector saved = repository.save(collector);
        log.info("Third-party collector approved: empId={}, email={}", saved.getEmpId(), saved.getEmail());

        if (firstCredentials && tempPassword != null) {
            AdminCreationLogger.log(saved.getEmail(), tempPassword);
            try {
                emailService.sendAdminCredentials(saved.getEmail(), tempPassword);
            } catch (Exception e) {
                log.warn("Failed to send credentials email to {}: {}", saved.getEmail(), e.getMessage());
            }
        } else {
            try {
                emailService.sendRegistrationApproved(saved.getEmail(), saved.getEmpName());
            } catch (Exception e) {
                log.warn("Failed to send approval email to {}: {}", saved.getEmail(), e.getMessage());
            }
        }
        return saved;
    }

    public ThirdPartyCollector reject(Long empId, String reason) {
        ThirdPartyCollector collector = repository.findById(empId)
                .orElseThrow(() -> new IllegalArgumentException("Third-party collector not found with id: " + empId));

        if (collector.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new IllegalStateException(
                    "Third-party collector is not in PENDING status. Current: " + collector.getRegistrationStatus());
        }

        collector.setRegistrationStatus(RegistrationStatus.REJECTED);
        ThirdPartyCollector saved = repository.save(collector);
        log.info("Third-party collector rejected: empId={}, email={}, reason={}", saved.getEmpId(), saved.getEmail(), reason);
        try {
            emailService.sendRegistrationRejected(saved.getEmail(), saved.getEmpName(), reason);
        } catch (Exception e) {
            log.warn("Failed to send rejection email to {}: {}", saved.getEmail(), e.getMessage());
        }
        return saved;
    }

    public ThirdPartyCollector setPassword(Long empId, String email, String password) {
        ThirdPartyCollector collector = repository.findById(empId)
                .orElseThrow(() -> new IllegalArgumentException("Third-party collector not found with id: " + empId));

        if (!collector.getEmail().equalsIgnoreCase(email)) {
            throw new IllegalArgumentException("Email does not match the third-party collector record");
        }

        if (collector.getRegistrationStatus() != RegistrationStatus.APPROVED) {
            throw new IllegalStateException("Third-party collector registration is not yet approved");
        }

        if (collector.getPassword() != null && !collector.getPassword().isBlank()) {
            throw new IllegalStateException("Password has already been set. Use change-password instead.");
        }

        collector.setPassword(passwordEncoder.encode(password));
        collector.setMustChangePassword(false);
        ThirdPartyCollector saved = repository.save(collector);
        log.info("Third-party collector password set: empId={}, email={}", saved.getEmpId(), saved.getEmail());
        return saved;
    }

    public Optional<ThirdPartyCollector> getRegistrationStatus(Long empId) {
        return repository.findById(empId);
    }

    public List<ThirdPartyCollector> getByStatus(RegistrationStatus status) {
        return repository.findByRegistrationStatus(status);
    }

    public List<ThirdPartyCollector> getPendingForCouncil(String council) {
        List<ThirdPartyCollector> pending = repository.findByRegistrationStatus(RegistrationStatus.PENDING);
        if (council == null || council.isBlank()) {
            return pending;
        }
        String councilLower = council.trim().toLowerCase();
        return pending.stream()
                .filter(c -> {
                    String assigned = c.getAssignedCouncils();
                    if (assigned == null || assigned.isBlank()) {
                        return false;
                    }
                    for (String part : assigned.split(",")) {
                        if (part.trim().equalsIgnoreCase(councilLower)) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
    }

    public List<String> getAvailableCouncils() {
        return councilRepository.findByIsActiveTrue().stream().map(Council::getName).toList();
    }

    private String generateTemporaryPassword(int length) {
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = rnd.nextInt(PASSWORD_ALPHABET.length());
            sb.append(PASSWORD_ALPHABET.charAt(idx));
        }
        return sb.toString();
    }
}
