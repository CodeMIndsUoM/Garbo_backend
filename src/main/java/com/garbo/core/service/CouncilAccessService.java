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

        String normalizedEmail = UserLookup.normalizeEmail(email);

        Optional<Citizen> citizen = citizenRepository.findFirstByEmailIgnoreCase(normalizedEmail);
        if (citizen.isPresent()) {
            Optional<String> council = councilFromCitizen(citizen.get());
            if (council.isPresent()) {
                return council;
            }
        }

        Optional<User> user = userRepository.findFirstByEmailIgnoreCase(normalizedEmail);
        if (user.isPresent() && user.get() instanceof Citizen citizenUser) {
            Optional<String> council = councilFromCitizen(citizenUser);
            if (council.isPresent()) {
                return council;
            }
        }

        Optional<AdminNew> admin = adminNewRepository.findFirstByEmailIgnoreCase(normalizedEmail);
        if (admin.isPresent()) {
            return normalizeCouncil(admin.get().getCouncil());
        }

        return Optional.empty();
    }

    private Optional<String> councilFromCitizen(Citizen citizen) {
        Optional<String> direct = normalizeCouncil(citizen.getCouncil());
        if (direct.isPresent()) {
            return direct;
        }
        try {
            if (citizen.getCouncilEntity() != null) {
                return normalizeCouncil(citizen.getCouncilEntity().getName());
            }
        } catch (Exception ignored) {
            // Lazy councilEntity may be unavailable outside a session; ignore.
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
