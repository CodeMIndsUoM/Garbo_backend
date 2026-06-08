package com.garbo.core.service;

import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.User;
import com.garbo.core.repository.AdminNewRepository;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class CouncilAccessService {

    private final UserRepository userRepository;
    private final CitizenRepository citizenRepository;
    private final AdminNewRepository adminNewRepository;

    public CouncilAccessService(UserRepository userRepository,
            CitizenRepository citizenRepository,
            AdminNewRepository adminNewRepository) {
        this.userRepository = userRepository;
        this.citizenRepository = citizenRepository;
        this.adminNewRepository = adminNewRepository;
    }

    public Optional<String> resolveCouncilForEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        Optional<Citizen> citizen = citizenRepository.findFirstByEmailIgnoreCase(email);
        if (citizen.isPresent()) {
            return normalizeCouncil(citizen.get().getCouncil());
        }

        Optional<AdminNew> admin = adminNewRepository.findFirstByEmailIgnoreCase(email);
        if (admin.isPresent()) {
            return normalizeCouncil(admin.get().getCouncil());
        }

        return Optional.empty();
    }

    public boolean isSuperAdmin(String email) {
        Optional<User> userOpt = userRepository.findFirstByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return false;
        }
        String role = userOpt.get().getRole();
        if (role == null || role.isBlank()) {
            return false;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("SUPERADMIN") || normalized.equals("ROLE_SUPERADMIN");
    }

    public boolean isAdmin(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (isSuperAdmin(email)) {
            return true;
        }
        return adminNewRepository.findFirstByEmailIgnoreCase(email).isPresent();
    }

    private Optional<String> normalizeCouncil(String council) {
        if (council == null) {
            return Optional.empty();
        }
        String normalized = council.trim();
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }
}
