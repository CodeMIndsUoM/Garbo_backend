package com.garbo.core.repository;

import com.garbo.core.entity.CouncilBoundary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouncilBoundaryRepository extends JpaRepository<CouncilBoundary, Long> {

    Optional<CouncilBoundary> findByCouncilIgnoreCase(String council);
}