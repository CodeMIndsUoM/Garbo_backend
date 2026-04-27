package com.garbo.core.service;

import com.garbo.core.entity.Admin;
import com.garbo.core.entity.User;
import com.garbo.core.repository.AdminRepository;
import com.garbo.core.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AdminService {
    @Autowired
    private AdminRepository adminRepo;
    @Autowired
    private UserRepository userRepo;

    public Admin saveAdmin(Admin admin) {
        return adminRepo.save(admin);
    }

    public Optional<Admin> login(String email, String password) {
        Optional<User> userOpt = userRepo.findFirstByEmailAndPasswordOrderByEmpIdAsc(email, password);
        if (userOpt.isPresent()) {
            Long empId = userOpt.get().getEmpId();
            return adminRepo.findById(empId);
        }
        return Optional.empty();
    }

    public List<Admin> getAllAdmins() {
        return adminRepo.findAll();
    }
}
