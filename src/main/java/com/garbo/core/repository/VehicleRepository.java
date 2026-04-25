package com.garbo.core.repository;

import com.garbo.core.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    // ── KPI: Total active fleet ───────────────────────────────────────────────
    long countByIsActiveTrue();

    // ── KPI: Count by status (active vehicles only) ───────────────────────────
    long countByStatusAndIsActiveTrue(String status);

    // ── Table: All active vehicles for the fleet list ─────────────────────────
    List<Vehicle> findAllByIsActiveTrueOrderByVehicleCodeAsc();

    // ── Optional: filter by status for table ──────────────────────────────────
    List<Vehicle> findAllByStatusAndIsActiveTrueOrderByVehicleCodeAsc(String status);
}