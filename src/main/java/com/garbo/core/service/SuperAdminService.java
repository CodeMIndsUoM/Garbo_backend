package com.garbo.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.garbo.core.entity.SuperAdmin;
import com.garbo.core.entity.User;
import com.garbo.core.repository.SuperAdminRepository;
import com.garbo.core.repository.UserRepository;

@Service
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepo;
    private final UserRepository userRepository;

    public SuperAdminService(SuperAdminRepository superAdminRepo, UserRepository userRepository) {
        this.superAdminRepo = superAdminRepo;
        this.userRepository = userRepository;
    }

    public SuperAdmin saveSuperAdmin(SuperAdmin superAdmin) {
        return this.superAdminRepo.save(superAdmin);
    }

    public Optional<SuperAdmin> login(String email, String password) {
        Optional<User> userOpt = userRepository.findFirstByEmailAndPasswordOrderByEmpIdAsc(email, password);
        if (userOpt.isPresent()) {
            Long empId = userOpt.get().getEmpId();
            return superAdminRepo.findById(empId);
        }
        return Optional.empty();
    }

    public List<SuperAdmin> getAllSuperAdmins() {
        return this.superAdminRepo.findAll();
    }
}
