package com.garbo.core.repository;

import com.garbo.core.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {


	// Find admin by email and password
	Optional<Admin> findByEmailAndPassword(String email, String password);
}
