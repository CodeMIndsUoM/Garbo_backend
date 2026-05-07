package com.garbo.core.repository;

import com.garbo.core.entity.Council;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CouncilRepository extends JpaRepository<Council, Long> {
    Optional<Council> findByNameIgnoreCase(String name);
    List<Council> findByIsActiveTrue();
}
