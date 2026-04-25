package com.garbo.core.service;

import com.garbo.core.entity.Driver;
import com.garbo.core.exception.DuplicateResourceException;
import com.garbo.core.exception.ResourceNotFoundException;
import com.garbo.core.repository.DriverRepository;
import com.garbo.core.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.List;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public Driver createDriver(Driver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Driver code and name are required");
        }

        if (driver.getDriverCode() == null || driver.getDriverCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Driver code is required");
        }

        if (driver.getName() == null || driver.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Driver name is required");
        }

        String code = driver.getDriverCode().trim();
        String name = driver.getName().trim();
        if (driverRepository.existsByDriverCode(code)) {
            throw new DuplicateResourceException("Driver code already exists: " + code);
        }

        driver.setId(null);
        driver.setDriverCode(code);
        driver.setName(name);
        return driverRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public List<Driver> getAllDrivers() {
        return driverRepository.findAll();
    }

    @Transactional
    public Driver updateDriver(Long id, Driver update) {
        Objects.requireNonNull(id, "id");
        Driver existing = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        Objects.requireNonNull(existing, "existing");

        if (update == null) {
            throw new IllegalArgumentException("Driver code and name are required");
        }

        if (update.getDriverCode() == null || update.getDriverCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Driver code is required");
        }

        if (update.getName() == null || update.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Driver name is required");
        }

        String code = update.getDriverCode().trim();
        String name = update.getName().trim();

        if (driverRepository.existsByDriverCodeAndIdNot(code, id)) {
            throw new DuplicateResourceException("Driver code already exists: " + code);
        }

        existing.setDriverCode(code);
        existing.setName(name);
        return driverRepository.save(existing);
    }

    @Transactional
    public void deleteDriver(Long id) {
        Objects.requireNonNull(id, "id");
        Driver existing = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with id: " + id));
        Objects.requireNonNull(existing, "existing");

        vehicleRepository.unassignDriverFromVehicles(id);
        driverRepository.delete(existing);
    }
}
