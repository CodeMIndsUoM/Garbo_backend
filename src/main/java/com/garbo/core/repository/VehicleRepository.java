package com.garbo.core.repository;

import com.garbo.core.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // ── Basic finders (HEAD) ──────────────────────────────────────────────────

    List<Vehicle> findByAssignedCouncil(String assignedCouncil);




    // ── Dashboard analytics (kevin-RWS) ──────────────────────────────────────

    // Total active fleet
    long countByIsActiveTrue();

    // Count by status (active vehicles only)
    long countByStatusAndIsActiveTrue(String status);

    // Table: All active vehicles for the fleet list
    List<Vehicle> findAllByIsActiveTrueOrderByIdAsc();

    // Optional: filter by status for table
    List<Vehicle> findAllByStatusAndIsActiveTrueOrderByIdAsc(String status);
}