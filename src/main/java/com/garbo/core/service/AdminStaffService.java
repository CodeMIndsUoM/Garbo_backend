package com.garbo.core.service;

import com.garbo.api.dto.staff.StaffCreateRequest;
import com.garbo.api.dto.staff.StaffUpdateRequest;
import com.garbo.api.dto.staff.UserSummaryDto;
import com.garbo.api.dto.staff.StaffListDto;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.User;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.email.EmailService;
import com.garbo.common.logging.AdminCreationLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AdminStaffService {

    private static final Logger log = LoggerFactory.getLogger(AdminStaffService.class);

    private final FieldMentorRepository fieldMentorRepository;
    private final BinCollectorRepository binCollectorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AdminStaffService(FieldMentorRepository fieldMentorRepository,
            BinCollectorRepository binCollectorRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.fieldMentorRepository = fieldMentorRepository;
        this.binCollectorRepository = binCollectorRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public Optional<UserSummaryDto> createFieldMentor(StaffCreateRequest req, String adminCouncil) {
        if (req == null || req.getEmail() == null || req.getFullName() == null)
            return Optional.empty();

        String email = req.getEmail().trim();
        if (userRepository.findFirstByEmailIgnoreCase(email).isPresent()) {
            return Optional.empty();
        }

        FieldMentor fm = new FieldMentor();
        fm.setEmpName(req.getFullName());
        fm.setEmail(email);
        fm.setPhone(req.getContactNumber());
        fm.setAssignedCouncil(adminCouncil);
        fm.setRole("FIELD_MENTOR");
        fm.setOnDuty(false);
        fm.setRewardPoints(0.0);
        fm.setCreatedAt(LocalDateTime.now());

        String temp = generateTemporaryPassword(12);
        String hashed = passwordEncoder.encode(temp);
        fm.setPassword(hashed);
        fm.setMustChangePassword(true);

        FieldMentor saved = fieldMentorRepository.save(fm);

        AdminCreationLogger.log(saved.getEmail(), temp);
        try {
            emailService.sendAdminCredentials(saved.getEmail(), temp);
        } catch (Exception ex) {
            log.error("Failed to send staff credentials email to {}", saved.getEmail(), ex);
        }

        return Optional.of(mapToDto(saved));
    }

    public Optional<UserSummaryDto> createBinCollector(StaffCreateRequest req, String adminCouncil) {
        if (req == null || req.getEmail() == null || req.getFullName() == null)
            return Optional.empty();

        String email = req.getEmail().trim();
        if (userRepository.findFirstByEmailIgnoreCase(email).isPresent()) {
            return Optional.empty();
        }

        BinCollector bc = new BinCollector();
        bc.setEmpName(req.getFullName());
        bc.setEmail(email);
        bc.setPhone(req.getContactNumber());
        bc.setAssignedCouncil(adminCouncil);
        bc.setRole("BIN_COLLECTOR");
        bc.setOnDuty(false);
        bc.setRewardPoints(0.0);
        bc.setCompletedCollections(0);
        bc.setMissedCollections(0);
        bc.setCreatedAt(LocalDateTime.now());

        String temp = generateTemporaryPassword(12);
        String hashed = passwordEncoder.encode(temp);
        bc.setPassword(hashed);
        bc.setMustChangePassword(true);

        BinCollector saved = binCollectorRepository.save(bc);

        AdminCreationLogger.log(saved.getEmail(), temp);
        try {
            emailService.sendAdminCredentials(saved.getEmail(), temp);
        } catch (Exception ex) {
            log.error("Failed to send staff credentials email to {}", saved.getEmail(), ex);
        }

        return Optional.of(mapToDto(saved));
    }

    public List<StaffListDto> listStaffForCurrentAdmin(String adminCouncil) {
        List<StaffListDto> out = new ArrayList<>();

        // If adminCouncil == null -> interpret as superadmin request -> list all
        List<FieldMentor> mentors = adminCouncil == null
                ? fieldMentorRepository.findAll()
                : fieldMentorRepository.findByAssignedCouncil(adminCouncil);
        for (FieldMentor m : mentors) {
            if (!Boolean.TRUE.equals(m.getAdminHidden())) {
                out.add(mapToListDto(m));
            }
        }

        List<BinCollector> collectors = adminCouncil == null
                ? binCollectorRepository.findAll()
                : binCollectorRepository.findByAssignedCouncil(adminCouncil);
        for (BinCollector c : collectors) {
            if (!Boolean.TRUE.equals(c.getAdminHidden())) {
                out.add(mapToListDto(c));
            }
        }

        return out;
    }

    public Optional<UserSummaryDto> updateFieldMentor(Long empId, StaffUpdateRequest req, String adminCouncil) {
        return fieldMentorRepository.findById(empId)
                .flatMap(mentor -> applyStaffUpdate(mentor, req, adminCouncil));
    }

    public Optional<UserSummaryDto> updateBinCollector(Long empId, StaffUpdateRequest req, String adminCouncil) {
        return binCollectorRepository.findById(empId)
                .flatMap(collector -> applyStaffUpdate(collector, req, adminCouncil));
    }

    private Optional<UserSummaryDto> applyStaffUpdate(User user, StaffUpdateRequest req, String adminCouncil) {
        if (req == null || !canManageInternalUser(user, adminCouncil)) {
            return Optional.empty();
        }
        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setEmpName(req.getFullName().trim());
        }
        if (req.getContactNumber() != null) {
            user.setPhone(req.getContactNumber().trim());
        }
        if (adminCouncil == null && req.getCouncil() != null && !req.getCouncil().isBlank()) {
            if (user instanceof FieldMentor mentor) {
                mentor.setAssignedCouncil(req.getCouncil().trim());
            } else if (user instanceof BinCollector collector) {
                collector.setAssignedCouncil(req.getCouncil().trim());
            }
        }
        if (Boolean.TRUE.equals(req.getResetPassword())) {
            String temp = generateTemporaryPassword(12);
            user.setPassword(passwordEncoder.encode(temp));
            user.setMustChangePassword(true);
            try {
                emailService.sendAdminCredentials(user.getEmail(), temp);
            } catch (Exception ex) {
                log.error("Failed to email reset password to {}", user.getEmail(), ex);
            }
        }
        User saved = userRepository.save(user);
        return Optional.of(mapToDto(saved));
    }

    private StaffListDto mapToListDto(User u) {
        StaffListDto dto = new StaffListDto();
        dto.setEmpId(u.getEmpId());
        dto.setEmpName(u.getEmpName());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        // onDuty is applicable to both FieldMentor and BinCollector
        if (u instanceof BinCollector)
            dto.setOnDuty(((BinCollector) u).isOnDuty());
        if (u instanceof FieldMentor)
            dto.setOnDuty(((FieldMentor) u).isOnDuty());
        return dto;
    }

    public String hideInternalUser(Long id, String adminCouncil) {
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return "NOT_FOUND";
        }

        User u = opt.get();
        if (!canManageInternalUser(u, adminCouncil)) {
            return u instanceof FieldMentor || u instanceof BinCollector ? "FORBIDDEN" : "NOT_INTERNAL";
        }

        if (u instanceof FieldMentor mentor) {
            mentor.setAdminHidden(true);
            fieldMentorRepository.save(mentor);
            return "HIDDEN";
        }
        if (u instanceof BinCollector collector) {
            collector.setAdminHidden(true);
            binCollectorRepository.save(collector);
            return "HIDDEN";
        }
        return "NOT_INTERNAL";
    }

    public String deleteInternalUser(Long id, String adminCouncil) {
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return "NOT_FOUND";
        }

        User u = opt.get();
        if (!canManageInternalUser(u, adminCouncil)) {
            return u instanceof FieldMentor || u instanceof BinCollector ? "FORBIDDEN" : "NOT_INTERNAL";
        }

        try {
            userRepository.deleteById(id);
            return "DELETED";
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.error("Failed to delete user {} due to constraint", id, ex);
            return "CONFLICT";
        } catch (Exception ex) {
            log.error("Unexpected error deleting user {}", id, ex);
            return "ERROR";
        }
    }

    private boolean canManageInternalUser(User user, String adminCouncil) {
        String assigned = null;
        if (user instanceof FieldMentor mentor) {
            assigned = mentor.getAssignedCouncil();
        } else if (user instanceof BinCollector collector) {
            assigned = collector.getAssignedCouncil();
        } else {
            return false;
        }

        if (adminCouncil == null) {
            return true;
        }
        return assigned != null && assigned.equals(adminCouncil);
    }

    private UserSummaryDto mapToDto(User u) {
        UserSummaryDto dto = new UserSummaryDto();
        dto.setEmpId(u.getEmpId());
        dto.setEmpName(u.getEmpName());
        dto.setEmail(u.getEmail());
        dto.setRole(u.getRole());
        if (u instanceof FieldMentor)
            dto.setAssignedCouncil(((FieldMentor) u).getAssignedCouncil());
        if (u instanceof BinCollector)
            dto.setAssignedCouncil(((BinCollector) u).getAssignedCouncil());
        dto.setMustChangePassword(u.isMustChangePassword());
        dto.setCreatedAt(u.getCreatedAt());
        if (u instanceof BinCollector)
            dto.setOnDuty(((BinCollector) u).isOnDuty());
        if (u instanceof BinCollector)
            dto.setRewardPoints(((BinCollector) u).getRewardPoints());
        if (u instanceof FieldMentor)
            dto.setOnDuty(((FieldMentor) u).isOnDuty());
        if (u instanceof FieldMentor)
            dto.setRewardPoints(((FieldMentor) u).getRewardPoints());
        return dto;
    }

    private static final String PASSWORD_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*()-_";

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
