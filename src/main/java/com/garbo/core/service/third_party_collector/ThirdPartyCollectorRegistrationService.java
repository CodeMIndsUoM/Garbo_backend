package com.garbo.core.service.third_party_collector;

import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.enums.RegistrationStatus;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ThirdPartyCollectorRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyCollectorRegistrationService.class);

    private final ThirdPartyCollectorRepository repository;
    private final PasswordEncoder passwordEncoder;

    public ThirdPartyCollectorRegistrationService(
            ThirdPartyCollectorRepository repository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
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
            String assignedCouncil) {

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
        collector.setAssignedCouncil(assignedCouncil);
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
        ThirdPartyCollector saved = repository.save(collector);
        log.info("Third-party collector approved: empId={}, email={}", saved.getEmpId(), saved.getEmail());
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

    public List<String> getAvailableCouncils() {
        return repository.findDistinctCouncils();
    }
}
