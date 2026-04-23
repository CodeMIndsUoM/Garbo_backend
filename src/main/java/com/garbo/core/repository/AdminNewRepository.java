package com.garbo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.garbo.core.entity.AdminNew;

@Repository
public interface AdminNewRepository extends JpaRepository<AdminNew, Long> {
    // additional query methods for AdminNew can be added here if needed
}
