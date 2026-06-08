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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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

    public List<ThirdPartyCollector> getActiveCollectors(String councilFilter) {
        return repository.findAll().stream()
                .filter(collector -> !Boolean.TRUE.equals(collector.getAdminHidden()))
                .filter(this::isActiveCollector)
                .filter(collector -> matchesCouncil(councilFilter, collector.getAssignedCouncils()))
                .toList();
    }

    public List<ThirdPartyCollector> getRevokedCollectors(String councilFilter) {
        return repository.findAll().stream()
                .filter(collector -> !Boolean.TRUE.equals(collector.getAdminHidden()))
                .filter(this::isRevokedCollector)
                .filter(collector -> matchesCouncil(councilFilter, collector.getAssignedCouncils()))
                .toList();
    }

    public List<ThirdPartyCollector> getPendingCollectors(String councilFilter) {
        return getByStatus(RegistrationStatus.PENDING).stream()
                .filter(collector -> !Boolean.TRUE.equals(collector.getAdminHidden()))
                .filter(collector -> matchesCouncil(councilFilter, collector.getAssignedCouncils()))
                .toList();
    }

    public ThirdPartyCollector getCollector(Long empId) {
        return repository.findById(empId)
                .orElseThrow(() -> new IllegalArgumentException("Third-party collector not found with id: " + empId));
    }

    public ThirdPartyCollector revoke(Long empId, String reason) {
        ThirdPartyCollector collector = getCollector(empId);
        if (!isActiveCollector(collector)) {
            throw new IllegalStateException(
                    "Only active third-party collectors can be revoked. Current: " + collector.getRegistrationStatus());
        }
        collector.setAdminRevoked(true);
        if (collector.getRegistrationStatus() == null) {
            collector.setRegistrationStatus(RegistrationStatus.APPROVED);
        }
        ThirdPartyCollector saved = repository.save(collector);
        log.info("Third-party collector revoked: empId={}, email={}, reason={}",
                saved.getEmpId(), saved.getEmail(), reason);
        return saved;
    }

    public ThirdPartyCollector unrevoke(Long empId) {
        ThirdPartyCollector collector = getCollector(empId);
        if (!isRevokedCollector(collector)) {
            throw new IllegalStateException("Only revoked third-party collectors can be restored");
        }
        collector.setAdminRevoked(false);
        collector.setRegistrationStatus(RegistrationStatus.APPROVED);
        ThirdPartyCollector saved = repository.save(collector);
        log.info("Third-party collector restored: empId={}, email={}", saved.getEmpId(), saved.getEmail());
        return saved;
    }

    private boolean isActiveCollector(ThirdPartyCollector collector) {
        if (Boolean.TRUE.equals(collector.getAdminRevoked())) {
            return false;
        }
        RegistrationStatus status = collector.getRegistrationStatus();
        if (status == RegistrationStatus.PENDING || status == RegistrationStatus.REJECTED) {
            return false;
        }
        return status == RegistrationStatus.APPROVED || status == null;
    }

    private boolean isRevokedCollector(ThirdPartyCollector collector) {
        return Boolean.TRUE.equals(collector.getAdminRevoked());
    }

    private boolean matchesCouncil(String councilFilter, String assignedCouncils) {
        if (councilFilter == null || councilFilter.isBlank()) {
            return true;
        }
        if (assignedCouncils == null || assignedCouncils.isBlank()) {
            return false;
        }
        String needle = councilFilter.trim().toLowerCase();
        return Arrays.stream(assignedCouncils.split(","))
                .map(String::trim)
                .anyMatch(part -> part.equalsIgnoreCase(needle));
    }

    public List<String> getAvailableCouncils() {
        return councilRepository.findByIsActiveTrue().stream().map(Council::getName).toList();
    }

    public String hideCollector(Long empId) {
        ThirdPartyCollector collector = getCollector(empId);
        collector.setAdminHidden(true);
        repository.save(collector);
        log.info("Third-party collector hidden: empId={}, email={}", collector.getEmpId(), collector.getEmail());
        return "HIDDEN";
    }

    public String deleteCollector(Long empId) {
        if (!repository.existsById(empId)) {
            return "NOT_FOUND";
        }
        try {
            repository.deleteById(empId);
            log.info("Third-party collector deleted: empId={}", empId);
            return "DELETED";
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to delete third-party collector {} due to constraint", empId, ex);
            return "CONFLICT";
        } catch (Exception ex) {
            log.error("Unexpected error deleting third-party collector {}", empId, ex);
            return "ERROR";
        }
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
