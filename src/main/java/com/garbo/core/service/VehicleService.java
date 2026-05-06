package com.garbo.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.garbo.core.entity.Vehicle;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.VehicleRepository;
import com.garbo.core.repository.RouteAssignmentRepository;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BinCollectorRepository binCollectorRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;

    public VehicleService(VehicleRepository vehicleRepository, BinCollectorRepository binCollectorRepository, RouteAssignmentRepository routeAssignmentRepository) {
        this.vehicleRepository = vehicleRepository;
        this.binCollectorRepository = binCollectorRepository;
        this.routeAssignmentRepository = routeAssignmentRepository;
    }

    public List<Vehicle> getAll() {
        List<Vehicle> list = vehicleRepository.findAll();
        populateDriverNames(list);
        return list;
    }

    public List<Vehicle> getByCouncil(String council) {
        List<Vehicle> list = vehicleRepository.findByAssignedCouncil(council);
        populateDriverNames(list);
        return list;
    }

    public Vehicle create(Vehicle payload) {
        if (payload.getLicensePlate() == null || payload.getLicensePlate().isBlank()) {
            throw new IllegalArgumentException("License plate is required");
        }

        String council = payload.getAssignedCouncil();
        if (council == null || council.isBlank()) {
            council = CurrentUserService.getCurrentCouncil().orElse("Unassigned");
            payload.setAssignedCouncil(council);
        }
        
        // Removed vehicleCode generation as per user request

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
        existing.setUpdatedAt(LocalDateTime.now());
        normalizeVehicle(existing);
        validateDriver(existing.getAssignedDriverId());
        Vehicle saved = vehicleRepository.save(existing);
        populateDriverNames(List.of(saved));
        return saved;
    }

    public Vehicle updateStatus(Long id, String status) {
        Vehicle existing = vehicleRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Vehicle not found"));
        existing.setStatus(status);
        existing.setUpdatedAt(LocalDateTime.now());
        normalizeVehicle(existing);
        Vehicle saved = vehicleRepository.save(existing);
        populateDriverNames(List.of(saved));
        return saved;
    }

    public void delete(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new NoSuchElementException("Vehicle not found");
        }
        routeAssignmentRepository.deleteByVehicleId(id);
        vehicleRepository.deleteById(id);
    }

    private void validateDriver(Long driverId) {
        if (driverId != null && !binCollectorRepository.existsById(driverId)) {
            throw new IllegalArgumentException("Assigned bin collector (driver) does not exist");
        }
    }

    private void normalizeVehicle(Vehicle vehicle) {
        if (vehicle.getStatus() == null || vehicle.getStatus().isBlank()) {
            vehicle.setStatus("available");
        } else {
            vehicle.setStatus(vehicle.getStatus().trim().toLowerCase(Locale.ROOT));
        }
        if (vehicle.getIsActive() == null) {
            vehicle.setIsActive(true);
        }
    }

    private void populateDriverNames(List<Vehicle> vehicles) {
        List<Long> driverIds = vehicles.stream()
                .map(Vehicle::getAssignedDriverId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (driverIds.isEmpty()) return;

        java.util.Map<Long, String> driverMap = binCollectorRepository.findAllById(driverIds)
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.garbo.core.entity.BinCollector::getEmpId,
                        com.garbo.core.entity.BinCollector::getEmpName,
                        (existing, replacement) -> existing
                ));

        vehicles.forEach(v -> {
            if (v.getAssignedDriverId() != null) {
                v.setAssignedDriverName(driverMap.get(v.getAssignedDriverId()));
            }
        });
    }
}
