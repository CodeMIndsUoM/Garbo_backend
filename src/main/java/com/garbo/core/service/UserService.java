package com.garbo.core.service;

import com.garbo.core.entity.User;
import com.garbo.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    final private UserRepository userRepo;

    public UserService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public User saveUser(User user) {
        return this.userRepo.save(user);
    }

    public Optional<User> login(String email, String password) {
        if (email == null || password == null) return Optional.empty();
        String e = email.trim();
        String p = password.trim();

        // try derived query
        Optional<User> found = userRepo.findFirstByEmailAndPasswordOrderByEmpIdAsc(e, p);
        if (found.isPresent()) return found;

        // try native exact match (LIMIT 1)
        found = userRepo.findByEmailAndPasswordNative(e, p);
        if (found.isPresent()) return found;

        // try case-insensitive email lookup then compare password in Java
        Optional<User> byEmail = userRepo.findFirstByEmailIgnoreCase(e);
        if (byEmail.isPresent() && p.equals(byEmail.get().getPassword())) {
            return byEmail;
        }

        return Optional.empty();
    }

    public List<User> getAllUsers() {
        return this.userRepo.findAll();
    }

    public Optional<User> getByEmail(String email) {
        if (email == null) return Optional.empty();
        String e = email.trim();
        Optional<User> found = userRepo.findFirstByEmailIgnoreCase(e);
        if (found.isPresent()) return found;
        return userRepo.findByEmailNative(e);
    }

    public Optional<User> getUserById(Long id) {
        return userRepo.findById(id);
    }

    public User updateUser(Long id, User updatedDetails) {
        return userRepo.findById(id).map(user -> {
            if (updatedDetails.getEmpName() != null) user.setEmpName(updatedDetails.getEmpName());
            if (updatedDetails.getEmail() != null) user.setEmail(updatedDetails.getEmail());
            if (updatedDetails.getPhone() != null) user.setPhone(updatedDetails.getPhone());
            if (updatedDetails.getDefaultAddress() != null) user.setDefaultAddress(updatedDetails.getDefaultAddress());
            if (updatedDetails.getAvatarUrl() != null) user.setAvatarUrl(updatedDetails.getAvatarUrl());
            return userRepo.save(user);
        }).orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
}
