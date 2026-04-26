package com.garbo.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garbo.core.entity.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    Optional<Vehicle> findByVehicleCodeIgnoreCase(String vehicleCode);
}
