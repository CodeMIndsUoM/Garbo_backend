package com.garbo.core.service;

import com.garbo.api.dto.CitizenRegisterRequest;
import com.garbo.core.entity.Citizen;
import com.garbo.core.entity.Council;
import com.garbo.core.repository.CitizenRepository;
import com.garbo.core.repository.CouncilRepository;
import com.garbo.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class CitizenService {

    private final CitizenRepository citizenRepository;
    private final CouncilRepository councilRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CitizenService(
            CitizenRepository citizenRepository,
            CouncilRepository councilRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.citizenRepository = citizenRepository;
        this.councilRepository = councilRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Citizen registerCitizen(CitizenRegisterRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        if (request.getCouncil() == null || request.getCouncil().isBlank()) {
            throw new IllegalArgumentException("Council is required");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }

        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.findFirstByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        String councilName = request.getCouncil().trim();
        Optional<Council> councilOpt = councilRepository.findByNameIgnoreCase(councilName);
        if (councilOpt.isEmpty() || !councilOpt.get().isActive()) {
            throw new IllegalArgumentException("Invalid or inactive council");
        }

        Citizen citizen = new Citizen();
        citizen.setEmpName(request.getFullName().trim());
        citizen.setEmail(email);
        citizen.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        citizen.setPassword(passwordEncoder.encode(request.getPassword()));
        citizen.setRole("CITIZEN");
        citizen.setCouncil(councilName);
        citizen.setCouncilEntity(councilOpt.get());
        citizen.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        citizen.setArea(request.getArea() != null ? request.getArea().trim() : null);
        citizen.setCreatedAt(LocalDateTime.now());
        citizen.setMustChangePassword(false);

        return citizenRepository.save(citizen);
    }
}
