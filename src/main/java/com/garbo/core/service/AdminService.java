package com.garbo.core.service;

import com.garbo.core.entity.Admin;
import com.garbo.core.entity.User;
import com.garbo.core.repository.AdminRepository;
import com.garbo.core.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    final private AdminRepository adminRepo;
    final private UserRepository userRepository;
    final private PasswordEncoder passwordEncoder;

    public AdminService(AdminRepository adminRepo, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.adminRepo = adminRepo;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Admin saveAdmin(Admin admin) {
        return this.adminRepo.save(admin);
    }

    public Optional<Admin> login(String email, String password) {
        if (email == null || password == null)
            return Optional.empty();

        Optional<User> userOpt = userRepository.findFirstByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        String stored = user.getPassword();
        if (stored == null)
            return Optional.empty();

        if (passwordEncoder.matches(password, stored)) {
            return adminRepo.findById(user.getEmpId());
        }

        // Legacy plaintext fallback
        if (stored.equals(password)) {
            String hashed = passwordEncoder.encode(password);
            user.setPassword(hashed);
            userRepository.save(user);
            return adminRepo.findById(user.getEmpId());
        }

        return Optional.empty();
    }

    public List<Admin> getAllAdmins() {
        return this.adminRepo.findAll();
    }
}
