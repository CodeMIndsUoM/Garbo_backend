package com.garbo.modules.auth.superAdmin.service;

import com.garbo.modules.auth.superAdmin.model.SuperAdmin;
import com.garbo.modules.auth.superAdmin.repository.SuperAdminRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class SuperAdminService {
    private final SuperAdminRepository superadminRepo;

    public SuperAdminService(SuperAdminRepository superadminRepo) {
        this.superadminRepo = superadminRepo;
    }
    public SuperAdmin saveSuperAdmin(SuperAdmin superAdmin) {
        return this.superadminRepo.save(superAdmin);
    }
    public Optional<SuperAdmin> getSuperAdminById(Long id) {
        return superadminRepo.findById(id);
    }
}
