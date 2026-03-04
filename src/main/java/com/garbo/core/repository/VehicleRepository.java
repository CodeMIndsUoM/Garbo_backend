package com.garbo.core.repository;

import com.garbo.core.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByVehicleCode(String vehicleCode);

    boolean existsByVehicleCode(String vehicleCode);

    boolean existsByLicensePlate(String licensePlate);

    List<Vehicle> findByAssignedCouncil(String assignedCouncil);

    List<Vehicle> findByStatus(String status);

    List<Vehicle> findByIsActiveTrue();

    Optional<Vehicle> findByIdAndIsActiveTrue(Long id);

    List<Vehicle> findByAssignedCouncilAndIsActiveTrue(String assignedCouncil);
}
