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

    public Optional<User> updateUser(Long userId, User payload) {
        if (userId == null || payload == null) {
            return Optional.empty();
        }
        Optional<User> existingOpt = userRepo.findById(userId);
        if (existingOpt.isEmpty()) {
            return Optional.empty();
        }
        User existing = existingOpt.get();
        if (payload.getEmpName() != null) existing.setEmpName(payload.getEmpName());
        if (payload.getEmail() != null) existing.setEmail(payload.getEmail());
        if (payload.getRole() != null) existing.setRole(payload.getRole());
        if (payload.getPhone() != null) existing.setPhone(payload.getPhone());
        if (payload.getPassword() != null && !payload.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(payload.getPassword()));
        }
        return Optional.of(userRepo.save(existing));
    }

    public boolean deleteUser(Long userId) {
        if (userId == null || !userRepo.existsById(userId)) {
            return false;
        }
        userRepo.deleteById(userId);
        return true;
    }

    public void changePassword(String email, String oldPassword, String newPassword) {
        if (email == null || oldPassword == null || newPassword == null) {
            throw new IllegalArgumentException("email, oldPassword and newPassword are required");
        }

        User user = getByEmail(email)
                .orElseThrow(java.util.NoSuchElementException::new);

        String stored = user.getPassword();
        if (stored == null) {
            throw new IllegalArgumentException("Invalid current password");
        }

        boolean matches;
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            matches = passwordEncoder.matches(oldPassword, stored);
        } else {
            matches = stored.equals(oldPassword);
        }

        if (!matches) {
            throw new IllegalArgumentException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepo.save(user);
    }
}
