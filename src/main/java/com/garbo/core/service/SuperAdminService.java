package com.garbo.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.garbo.core.entity.SuperAdmin;
import com.garbo.core.entity.User;
import com.garbo.core.repository.SuperAdminRepository;
import com.garbo.core.repository.UserRepository;

@Service
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepo;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminService(SuperAdminRepository superAdminRepo, UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.superAdminRepo = superAdminRepo;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public SuperAdmin saveSuperAdmin(SuperAdmin superAdmin) {
        return this.superAdminRepo.save(superAdmin);
    }

    public Optional<SuperAdmin> login(String email, String password) {
        if (email == null || password == null)
            return Optional.empty();

        Optional<User> userOpt = userRepository.findFirstByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmailNative(email);
            if (userOpt.isEmpty())
                return Optional.empty();
        }

        User user = userOpt.get();
        String stored = user.getPassword();
        if (stored == null)
            return Optional.empty();

        if (passwordEncoder.matches(password, stored)) {
            return superAdminRepo.findById(user.getEmpId());
        }

        if (stored.equals(password)) {
            String hashed = passwordEncoder.encode(password);
            user.setPassword(hashed);
            userRepository.save(user);
            return superAdminRepo.findById(user.getEmpId());
        }

        return Optional.empty();
    }

    public List<SuperAdmin> getAllSuperAdmins() {
        return this.superAdminRepo.findAll();
    }
}
