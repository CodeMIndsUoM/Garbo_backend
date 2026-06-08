
package com.garbo.core.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.entity.User;
import com.garbo.core.repository.AdminNewRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.email.EmailService;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepo;
    private final AdminNewRepository adminNewRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(
            UserRepository userRepo,
            AdminNewRepository adminNewRepo,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.userRepo = userRepo;
        this.adminNewRepo = adminNewRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User saveUser(User user) {
        return this.userRepo.save(user);
    }

    public AdminNew saveAdminNew(AdminNew adminNew) {
        if (adminNew.getPassword() == null || adminNew.getPassword().isBlank()) {
            String temp = generateTemporaryPassword(12);
            String hashed = passwordEncoder.encode(temp);

            com.garbo.common.logging.AdminCreationLogger.log(
                    adminNew.getEmail(),
                    temp);

            adminNew.setPassword(hashed);
            adminNew.setMustChangePassword(true);

            AdminNew saved = this.adminNewRepo.save(adminNew);

            try {
                emailService.sendAdminCredentials(saved.getEmail(), temp);
            } catch (Exception ex) {
                // Do not fail admin creation when SMTP/mail provider is unavailable.
                log.error("Admin created, but failed to send credentials email to {}", saved.getEmail(), ex);
            }

            return saved;
        } else {
            String hashed = passwordEncoder.encode(adminNew.getPassword());
            adminNew.setPassword(hashed);
        }

        return this.adminNewRepo.save(adminNew);
    }

    public Optional<User> login(String email, String password) {
        if (email == null || password == null) {
            return Optional.empty();
        }

        String e = email.trim();
        String p = password.trim();

        try {
            Optional<User> byEmail = userRepo.findFirstByEmailIgnoreCase(e);

            if (byEmail.isEmpty()) {
                return Optional.empty();
            }

            User user = byEmail.get();
            String stored = user.getPassword();

            if (stored == null) {
                return Optional.empty();
            }

            // BCrypt match
            if (passwordEncoder.matches(p, stored)) {
                return Optional.of(user);
            }

            // Legacy plaintext fallback + upgrade
            if (stored.equals(p)) {
                String hashed = passwordEncoder.encode(p);
                user.setPassword(hashed);
                userRepo.save(user);
                return Optional.of(user);
            }

            return Optional.empty();

        } catch (Exception ex) {
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
        if (email == null) {
            return Optional.empty();
        }

        String e = email.trim();

        Optional<User> found = userRepo.findFirstByEmailIgnoreCase(e);
        if (found.isPresent()) {
            return found;
        }

        return Optional.empty();
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

        if (payload.getEmpName() != null) {
            existing.setEmpName(payload.getEmpName());
        }

        if (payload.getEmail() != null) {
            existing.setEmail(payload.getEmail());
        }

        if (payload.getRole() != null) {
            existing.setRole(payload.getRole());
        }

        if (payload.getPhone() != null) {
            existing.setPhone(payload.getPhone());
        }

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

    public void changePassword(
            String email,
            String oldPassword,
            String newPassword) {

        if (email == null || oldPassword == null || newPassword == null) {
            throw new IllegalArgumentException(
                    "email and passwords must be provided");
        }

        Optional<User> found = getByEmail(email);

        if (found.isEmpty()) {
            throw new java.util.NoSuchElementException("User not found");
        }

        User user = found.get();
        String stored = user.getPassword();

        if (stored == null) {
            throw new IllegalArgumentException("Invalid current password");
        }

        boolean matches = passwordEncoder.matches(oldPassword, stored)
                || stored.equals(oldPassword);

        if (!matches) {
            throw new IllegalArgumentException("Invalid current password");
        }

        boolean firstLogin = user.isMustChangePassword();
        String hashed = passwordEncoder.encode(newPassword);

        user.setPassword(hashed);
        user.setMustChangePassword(false);

        if (firstLogin) {
            if (user instanceof FieldMentor mentor) {
                mentor.setOnDuty(true);
            } else if (user instanceof BinCollector collector) {
                collector.setOnDuty(true);
            }
        }

        userRepo.save(user);
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
// package com.garbo.core.service;

// import java.util.List;
// import java.util.Optional;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.stereotype.Service;

// import com.garbo.core.entity.User;
// import com.garbo.core.repository.UserRepository;

// @Service
// public class UserService {
// @Autowired
// private UserRepository userRepo;

// private final BCryptPasswordEncoder passwordEncoder = new
// BCryptPasswordEncoder();

// public User saveUser(User user) {
// return this.userRepo.save(user);
// }

// public Optional<User> login(String email, String password) {
// if (email == null || password == null) {
// return Optional.empty();
// }

// String normalizedEmail = email.trim();
// String normalizedPassword = password.trim();

// try {
// // Use the simplest query path first to avoid repository/query exceptions.
// Optional<User> byEmail =
// userRepo.findFirstByEmailIgnoreCase(normalizedEmail);
// if (byEmail.isPresent()) {
// String storedPassword = byEmail.get().getPassword();
// if (storedPassword != null) {
// boolean plainMatch = normalizedPassword.equals(storedPassword);
// boolean bcryptMatch = false;

// // Support bcrypt-hashed passwords used in current database.
// if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") ||
// storedPassword.startsWith("$2y$")) {
// bcryptMatch = passwordEncoder.matches(normalizedPassword, storedPassword);
// }

// if (plainMatch || bcryptMatch) {
// return byEmail;
// }
// }
// }

// return Optional.empty();
// } catch (Exception ex) {
// // Keep auth predictable: invalid credentials should never become a 500.
// return Optional.empty();
// }
// }

// public List<User> getAllUsers() {
// return this.userRepo.findAll();
// }

// public Optional<User> getById(Long userId) {
// if (userId == null) {
// return Optional.empty();
// }
// return userRepo.findById(userId);
// }

// public Optional<User> getByEmail(String email) {
// if (email == null) return Optional.empty();
// String e = email.trim();
// Optional<User> found = userRepo.findFirstByEmailIgnoreCase(e);
// if (found.isPresent()) return found;
// return userRepo.findByEmailNative(e);
// }

// public Optional<User> updateUser(Long userId, User payload) {
// if (userId == null || payload == null) {
// return Optional.empty();
// }
// Optional<User> existingOpt = userRepo.findById(userId);
// if (existingOpt.isEmpty()) {
// return Optional.empty();
// }
// User existing = existingOpt.get();
// if (payload.getEmpName() != null) existing.setEmpName(payload.getEmpName());
// if (payload.getEmail() != null) existing.setEmail(payload.getEmail());
// if (payload.getRole() != null) existing.setRole(payload.getRole());
// if (payload.getPhone() != null) existing.setPhone(payload.getPhone());
// if (payload.getPassword() != null && !payload.getPassword().isBlank()) {
// existing.setPassword(passwordEncoder.encode(payload.getPassword()));
// }
// return Optional.of(userRepo.save(existing));
// }

// public boolean deleteUser(Long userId) {
// if (userId == null || !userRepo.existsById(userId)) {
// return false;
// }
// userRepo.deleteById(userId);
// return true;
// }

// public void changePassword(String email, String oldPassword, String
// newPassword) {
// if (email == null || oldPassword == null || newPassword == null) {
// throw new IllegalArgumentException("email, oldPassword and newPassword are
// required");
// }

// User user = getByEmail(email)
// .orElseThrow(java.util.NoSuchElementException::new);

// String stored = user.getPassword();
// if (stored == null) {
// throw new IllegalArgumentException("Invalid current password");
// }

// boolean matches;
// if (stored.startsWith("$2a$") || stored.startsWith("$2b$") ||
// stored.startsWith("$2y$")) {
// matches = passwordEncoder.matches(oldPassword, stored);
// } else {
// matches = stored.equals(oldPassword);
// }

// if (!matches) {
// throw new IllegalArgumentException("Invalid current password");
// }

// user.setPassword(passwordEncoder.encode(newPassword));
// user.setMustChangePassword(false);
// userRepo.save(user);
// }
// }
