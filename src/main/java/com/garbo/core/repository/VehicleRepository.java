package com.garbo.core.repository;

import com.garbo.core.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    List<Vehicle> findByAssignedCouncil(String assignedCouncil);

    java.util.Optional<Vehicle> findFirstByAssignedDriverId(Long driverId);

    // ── Dashboard analytics ──────────────────────────────────────────────────

    long countByIsActiveTrue();
    long countByStatusAndIsActiveTrue(String status);
    List<Vehicle> findAllByIsActiveTrueOrderByIdAsc();
    List<Vehicle> findAllByStatusAndIsActiveTrueOrderByIdAsc(String status);

    // ── Council-filtered analytics ───────────────────────────────────────────

    long countByIsActiveTrueAndAssignedCouncil(String assignedCouncil);
    long countByStatusAndIsActiveTrueAndAssignedCouncil(String status, String assignedCouncil);
    List<Vehicle> findAllByIsActiveTrueAndAssignedCouncilOrderByIdAsc(String assignedCouncil);
    List<Vehicle> findAllByStatusAndIsActiveTrueAndAssignedCouncilOrderByIdAsc(String status, String assignedCouncil);
}