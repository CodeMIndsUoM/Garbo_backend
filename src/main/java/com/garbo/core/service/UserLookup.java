package com.garbo.core.service;

import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.User;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.UserRepository;

import java.util.Locale;
import java.util.Optional;

public final class UserLookup {

    private UserLookup() {
    }

    public static String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static User requireUser(UserRepository userRepository, String email) {
        return findUser(userRepository, email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public static Optional<User> findUser(UserRepository userRepository, String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return userRepository.findFirstByEmailIgnoreCase(normalized);
    }

    public static Citizen requireCitizen(CitizenRepository citizenRepository, String email) {
        return findCitizen(citizenRepository, email)
                .orElseThrow(() -> new RuntimeException("Citizen profile not found"));
    }

    public static Optional<Citizen> findCitizen(CitizenRepository citizenRepository, String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        return citizenRepository.findFirstByEmailIgnoreCase(normalized);
    }

    public static String resolveCitizenCouncil(Citizen citizen) {
        if (citizen.getCouncil() != null && !citizen.getCouncil().isBlank()) {
            return citizen.getCouncil().trim();
        }
        try {
            if (citizen.getCouncilEntity() != null && citizen.getCouncilEntity().getName() != null) {
                return citizen.getCouncilEntity().getName().trim();
            }
        } catch (Exception ignored) {
            // Lazy councilEntity may be unavailable outside a session.
        }
        return null;
    }
}
