package com.garbo.modules.auth.admin.service;

import com.garbo.modules.auth.admin.model.Admin;
import com.garbo.modules.auth.admin.repository.AdminRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminService {

    final private AdminRepository adminRepo;

    public AdminService(AdminRepository adminRepo) {
        this.adminRepo = adminRepo;
    }
    public Admin saveAdmin(Admin admin) {
        return this.adminRepo.save(admin);
    }
    public Optional<Admin> login(String email, String password) {
        return adminRepo.findByEmailAndPassword(email, password);
    }
    public List<Admin> getAllAdmins() {
        return this.adminRepo.findAll();
    }
//    public Optional<Admin> getAdminById(Long id) {
//        return adminRepo.findById(id);
//    }
}
