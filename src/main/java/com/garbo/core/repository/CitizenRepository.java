package com.garbo.core.repository;

import com.garbo.core.entity.Citizen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Long> {
    Optional<Citizen> findFirstByEmailIgnoreCase(String email);

    List<Citizen> findByCouncilIgnoreCase(String council);
}
