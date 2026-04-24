package com.garbo.core.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.garbo.core.entity.User;
import com.garbo.core.entity.AdminNew;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.repository.AdminNewRepository;
import com.garbo.infrastructure.email.EmailService;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final AdminNewRepository adminNewRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(UserRepository userRepo, AdminNewRepository adminNewRepo,
            PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepo = userRepo;
        this.adminNewRepo = adminNewRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public User saveUser(User user) {
        return this.userRepo.save(user);
    }

    /**
     * Save an AdminNew entity (JOINED inheritance will persist to `users` and
     * `admins_new`).
     *
     * This method generates a secure temporary password if none is provided,
     * hashes it with the configured PasswordEncoder (BCrypt) and stores only the
     * hashed password in the database.
     */
    public AdminNew saveAdminNew(AdminNew adminNew) {
        // If no password provided, generate a secure temporary password
        if (adminNew.getPassword() == null || adminNew.getPassword().isBlank()) {
            String temp = generateTemporaryPassword(12);
            String hashed = passwordEncoder.encode(temp);
            // Log to dev-only admin creation audit file
            com.garbo.common.logging.AdminCreationLogger.log(adminNew.getEmail(), temp);
            adminNew.setPassword(hashed);
            // Mark that the admin must change password on first login
            adminNew.setMustChangePassword(true);
            // Persist the user with hashed password before sending email
            AdminNew saved = this.adminNewRepo.save(adminNew);
            // Attempt to send email with credentials (dev: may be placeholder config)
            emailService.sendAdminCredentials(saved.getEmail(), temp);
            // Note: we do NOT return the temp password here; it is emailed to the admin.
            return saved;
        } else {
            // If caller provided a password (shouldn't happen for admin flow), hash it
            String hashed = passwordEncoder.encode(adminNew.getPassword());
            adminNew.setPassword(hashed);
        }

        return this.adminNewRepo.save(adminNew);
    }

    /**
     * Authenticate by email and raw password using PasswordEncoder.matches.
     * Also supports legacy plaintext-stored passwords by upgrading them to BCrypt
     * on first successful login.
     */
    public Optional<User> login(String email, String password) {
        if (email == null || password == null)
            return Optional.empty();
        String e = email.trim();
        String p = password.trim();

        Optional<User> byEmail = userRepo.findFirstByEmailIgnoreCase(e);
        if (byEmail.isEmpty()) {
            // last resort: native lookup (case-sensitive or legacy) without password
            Optional<User> nativeUser = userRepo.findByEmailNative(e);
            if (nativeUser.isEmpty())
                return Optional.empty();
            byEmail = nativeUser;
        }

        User user = byEmail.get();
        String stored = user.getPassword();
        if (stored == null)
            return Optional.empty();

        // If stored is BCrypt-hash, matches will succeed. If stored is plaintext,
        // matches(...) will return false; fall back to plaintext equality and
        // upgrade the stored password to a hashed value.
        if (passwordEncoder.matches(p, stored)) {
            return Optional.of(user);
        }

        // Legacy plaintext fallback: preserve backward compatibility and upgrade
        if (stored.equals(p)) {
            String hashed = passwordEncoder.encode(p);
            user.setPassword(hashed);
            userRepo.save(user);
            return Optional.of(user);
        }

        return Optional.empty();
    }

    public List<User> getAllUsers() {
        return this.userRepo.findAll();
    }

    public Optional<User> getByEmail(String email) {
        if (email == null)
            return Optional.empty();
        String e = email.trim();
        Optional<User> found = userRepo.findFirstByEmailIgnoreCase(e);
        if (found.isPresent())
            return found;
        return userRepo.findByEmailNative(e);
    }

    /**
     * Change a user's password after verifying their current password.
     * If the user is not found, a NoSuchElementException is thrown.
     * If the old password does not match, an IllegalArgumentException is thrown.
     */
    public void changePassword(String email, String oldPassword, String newPassword) {
        if (email == null || oldPassword == null || newPassword == null)
            throw new IllegalArgumentException("email and passwords must be provided");

        Optional<User> found = getByEmail(email);
        if (found.isEmpty())
            throw new java.util.NoSuchElementException("User not found");

        User user = found.get();
        String stored = user.getPassword();
        if (stored == null)
            throw new IllegalArgumentException("Invalid current password");

        // Verify current password. Support BCrypt hashes and legacy plaintext equality.
        boolean matches = passwordEncoder.matches(oldPassword, stored) || stored.equals(oldPassword);
        if (!matches)
            throw new IllegalArgumentException("Invalid current password");

        // Encode and save new password, clear mustChangePassword flag
        String hashed = passwordEncoder.encode(newPassword);
        user.setPassword(hashed);
        user.setMustChangePassword(false);
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
