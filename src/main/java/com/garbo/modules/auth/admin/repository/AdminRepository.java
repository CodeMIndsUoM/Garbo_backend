package com.garbo.modules.auth.admin.repository;

import com.garbo.modules.auth.admin.model.Admin;
import com.garbo.modules.auth.superAdmin.model.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {


	// Find admin by email and password
	Optional<Admin> findByEmailAndPassword(String email, String password);
}
