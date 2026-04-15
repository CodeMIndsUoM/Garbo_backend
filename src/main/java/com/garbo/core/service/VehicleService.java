package com.garbo.core.service;

import com.garbo.core.entity.Vehicle;
import com.garbo.core.exception.DuplicateResourceException;
import com.garbo.core.exception.ResourceNotFoundException;
import com.garbo.core.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicleRepository.existsByVehicleCode(vehicle.getVehicleCode())) {
            throw new DuplicateResourceException("Vehicle with code '" + vehicle.getVehicleCode() + "' already exists");
        }
        if (vehicleRepository.existsByLicensePlate(vehicle.getLicensePlate())) {
            throw new DuplicateResourceException("Vehicle with license plate '" + vehicle.getLicensePlate() + "' already exists");
        }
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Long id, Vehicle updatedVehicle) {
        Objects.requireNonNull(id, "id");
        Vehicle existing = vehicleRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        Objects.requireNonNull(existing, "existing");

        // Uniqueness check for vehicle code if changed
        if (updatedVehicle.getVehicleCode() != null && !existing.getVehicleCode().equals(updatedVehicle.getVehicleCode())) {
            if (vehicleRepository.existsByVehicleCode(updatedVehicle.getVehicleCode())) {
                throw new DuplicateResourceException("Vehicle code '" + updatedVehicle.getVehicleCode() + "' already exists");
            }
            existing.setVehicleCode(updatedVehicle.getVehicleCode());
        }

        // Uniqueness check for license plate if changed
        if (updatedVehicle.getLicensePlate() != null && !existing.getLicensePlate().equals(updatedVehicle.getLicensePlate())) {
            if (vehicleRepository.existsByLicensePlate(updatedVehicle.getLicensePlate())) {
                throw new DuplicateResourceException("License plate '" + updatedVehicle.getLicensePlate() + "' already exists");
            }
            existing.setLicensePlate(updatedVehicle.getLicensePlate());
        }

        if (updatedVehicle.getType() != null) existing.setType(updatedVehicle.getType());
        if (updatedVehicle.getCapacity() != null) existing.setCapacity(updatedVehicle.getCapacity());
        if (updatedVehicle.getStatus() != null) existing.setStatus(updatedVehicle.getStatus());
        if (updatedVehicle.getAssignedCouncil() != null) existing.setAssignedCouncil(updatedVehicle.getAssignedCouncil());
        if (updatedVehicle.getAssignedDriverId() != null) existing.setAssignedDriverId(updatedVehicle.getAssignedDriverId());
        if (updatedVehicle.getCurrentLocation() != null) existing.setCurrentLocation(updatedVehicle.getCurrentLocation());
        if (updatedVehicle.getFuelLevel() != null) existing.setFuelLevel(updatedVehicle.getFuelLevel());

        return vehicleRepository.save(existing);
    }

    public void deleteVehicle(Long id) {
        Objects.requireNonNull(id, "id");
        Vehicle existing = vehicleRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        Objects.requireNonNull(existing, "existing");
        vehicleRepository.delete(existing);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findByIdAndIsActiveTrue(id);
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByCouncil(String council) {
        return vehicleRepository.findByAssignedCouncilAndIsActiveTrue(council);
    }

    public Vehicle updateVehicleStatus(Long id, String status) {
        Vehicle existing = vehicleRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        existing.setStatus(status);
        return vehicleRepository.save(existing);
    }

    public Vehicle assignDriver(Long id, Long driverId) {
        Vehicle existing = vehicleRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        existing.setAssignedDriverId(driverId);
        return vehicleRepository.save(existing);
    }
}
