package com.garbo.core.repository;

import com.garbo.core.entity.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {

    // Find superadmin by email and password
    @Query("SELECT s FROM SuperAdmin s WHERE s.email = :email AND s.password = :password ORDER BY s.empId ASC LIMIT 1")
    Optional<SuperAdmin> findByEmailAndPassword(@Param("email") String email, @Param("password") String password);
}
