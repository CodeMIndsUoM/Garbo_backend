package com.garbo.core.repository;

import com.garbo.core.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {


	// Find admin by email and password (returns first match if duplicates exist)
	@Query("SELECT a FROM Admin a WHERE a.email = :email AND a.password = :password ORDER BY a.empId ASC LIMIT 1")
	Optional<Admin> findByEmailAndPassword(@Param("email") String email, @Param("password") String password);
}
