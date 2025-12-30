package com.garbo.modules.auth.superAdmin.controller;

import com.garbo.modules.auth.superAdmin.model.SuperAdmin;
import com.garbo.modules.auth.superAdmin.service.SuperAdminService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/superAdmin")
public class SuperAdminController {

    final private SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @PostMapping
    public void createSuperAdmin(@RequestBody SuperAdmin superAdmin) {
        System.out.println("admin saved successfully");
        superAdminService.saveSuperAdmin(superAdmin);
    }

    @GetMapping("/{empId}")
    public SuperAdmin getSuperAdminById(@PathVariable Long empId) {
        System.out.println("getting success");
        System.out.println(superAdminService.getSuperAdminById(empId));
        return superAdminService.getSuperAdminById(empId).orElse(null);
    }

}
