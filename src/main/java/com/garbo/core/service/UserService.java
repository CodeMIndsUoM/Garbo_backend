package com.garbo.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.garbo.core.entity.User;
import com.garbo.core.repository.UserRepository;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepo;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User saveUser(User user) {
        return this.userRepo.save(user);
    }

    public Optional<User> login(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }

        String normalizedEmail = email.trim();
        String normalizedPassword = password.trim();

        try {
            // Use the simplest query path first to avoid repository/query exceptions.
            Optional<User> byEmail = userRepo.findFirstByEmailIgnoreCase(normalizedEmail);
            if (byEmail.isPresent()) {
                String storedPassword = byEmail.get().getPassword();
                if (storedPassword != null) {
                    boolean plainMatch = normalizedPassword.equals(storedPassword);
                    boolean bcryptMatch = false;

                    // Support bcrypt-hashed passwords used in current database.
                    if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                        bcryptMatch = passwordEncoder.matches(normalizedPassword, storedPassword);
                    }

                    if (plainMatch || bcryptMatch) {
                        return byEmail;
                    }
                }
            }

            return Optional.empty();
        } catch (Exception ex) {
            // Keep auth predictable: invalid credentials should never become a 500.
            return Optional.empty();
        }
    }

    public List<User> getAllUsers() {
        return this.userRepo.findAll();
    }

    public Optional<User> getById(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return userRepo.findById(userId);
    }

    public Optional<User> getByEmail(String email) {
        if (email == null) return Optional.empty();
        String e = email.trim();
        Optional<User> found = userRepo.findFirstByEmailIgnoreCase(e);
        if (found.isPresent()) return found;
        return userRepo.findByEmailNative(e);
    }
}
