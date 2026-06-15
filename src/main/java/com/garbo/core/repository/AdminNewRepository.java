package com.garbo.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.garbo.core.entity.AdminNew;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminNewRepository extends JpaRepository<AdminNew, Long> {
    Optional<AdminNew> findFirstByEmailIgnoreCase(String email);

    List<AdminNew> findByCouncilIgnoreCase(String council);
}
