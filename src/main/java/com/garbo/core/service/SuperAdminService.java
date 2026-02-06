package com.garbo.core.service;

import com.garbo.core.entity.SuperAdmin;
import com.garbo.core.repository.SuperAdminRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepo;

    public SuperAdminService(SuperAdminRepository superAdminRepo) {
        this.superAdminRepo = superAdminRepo;
    }

    public SuperAdmin saveSuperAdmin(SuperAdmin superAdmin) {
        return this.superAdminRepo.save(superAdmin);
    }

    public Optional<SuperAdmin> login(String email, String password) {
        return superAdminRepo.findByEmailAndPassword(email, password);
    }

    public List<SuperAdmin> getAllSuperAdmins() {
        return this.superAdminRepo.findAll();
    }
}
