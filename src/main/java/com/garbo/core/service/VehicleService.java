package com.garbo.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.garbo.core.entity.Vehicle;
import com.garbo.core.repository.DriverRepository;
import com.garbo.core.repository.VehicleRepository;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public VehicleService(VehicleRepository vehicleRepository, DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    public List<Vehicle> getAll() {
        return vehicleRepository.findAll();
    }

    public Vehicle create(Vehicle payload) {
        if (payload.getVehicleCode() == null || payload.getVehicleCode().isBlank()) {
            throw new IllegalArgumentException("Vehicle code is required");
        }
        if (payload.getLicensePlate() == null || payload.getLicensePlate().isBlank()) {
            throw new IllegalArgumentException("License plate is required");
        }
        vehicleRepository.findByVehicleCodeIgnoreCase(payload.getVehicleCode())
                .ifPresent(v -> {
                    throw new IllegalArgumentException("Vehicle code already exists");
                });

        LocalDateTime now = LocalDateTime.now();
        payload.setCreatedAt(now);
        payload.setUpdatedAt(now);
        normalizeVehicle(payload);
        validateDriver(payload.getAssignedDriverId());
        return vehicleRepository.save(payload);
    }

    public Vehicle update(Long id, Vehicle payload) {
        Vehicle existing = vehicleRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Vehicle not found"));
        if (payload.getLicensePlate() != null) {
            existing.setLicensePlate(payload.getLicensePlate());
        }
        if (payload.getType() != null) {
            existing.setType(payload.getType());
        }
        existing.setCapacity(payload.getCapacity());
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus());
        }
        existing.setAssignedCouncil(payload.getAssignedCouncil());
        existing.setAssignedDriverId(payload.getAssignedDriverId());
        existing.setCurrentLocation(payload.getCurrentLocation());
        existing.setFuelLevel(payload.getFuelLevel());
        existing.setUpdatedAt(LocalDateTime.now());
        normalizeVehicle(existing);
        validateDriver(existing.getAssignedDriverId());
        return vehicleRepository.save(existing);
    }

    public Vehicle updateStatus(Long id, String status) {
        Vehicle existing = vehicleRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Vehicle not found"));
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        normalizeVehicle(existing);
        return vehicleRepository.save(existing);
    }

    public void delete(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new NoSuchElementException("Vehicle not found");
        }
        vehicleRepository.deleteById(id);
    }

    private void validateDriver(Long driverId) {
        if (driverId != null && !driverRepository.existsById(driverId)) {
            throw new IllegalArgumentException("Assigned driver does not exist");
        }
    }

    private void normalizeVehicle(Vehicle vehicle) {
        if (vehicle.getStatus() == null || vehicle.getStatus().isBlank()) {
            vehicle.setStatus("available");
        } else {
            vehicle.setStatus(vehicle.getStatus().trim().toLowerCase(Locale.ROOT));
        }
        if (vehicle.getFuelLevel() == null) {
            vehicle.setFuelLevel(100);
        } else {
            int bounded = Math.max(0, Math.min(vehicle.getFuelLevel(), 100));
            vehicle.setFuelLevel(bounded);
        }
        if (vehicle.getIsActive() == null) {
            vehicle.setIsActive(!"inactive".equalsIgnoreCase(vehicle.getStatus()));
        }
    }
}
