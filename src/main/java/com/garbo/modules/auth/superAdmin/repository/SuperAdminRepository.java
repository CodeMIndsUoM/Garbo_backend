
package com.garbo.modules.auth.superAdmin.repository;

import java.util.Optional;
import com.garbo.modules.auth.superAdmin.model.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {
	Optional<SuperAdmin> findByEmailAndPassword(String email, String password);
}