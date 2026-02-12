package com.garbo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.garbo.core.entity.SuperAdmin;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, Long> {

}
