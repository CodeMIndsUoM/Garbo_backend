package com.garbo.core.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.garbo.core.entity.Driver;
import com.garbo.core.repository.DriverRepository;

@Service
public class DriverService {

    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    public List<Driver> getAll() {
        return driverRepository.findAll();
    }

    public Driver create(Driver payload) {
        if (payload.getDriverCode() == null || payload.getDriverCode().isBlank()) {
            throw new IllegalArgumentException("Driver code is required");
        }
        if (payload.getName() == null || payload.getName().isBlank()) {
            throw new IllegalArgumentException("Driver name is required");
        }
        driverRepository.findByDriverCodeIgnoreCase(payload.getDriverCode())
                .ifPresent(d -> {
                    throw new IllegalArgumentException("Driver code already exists");
                });
        LocalDateTime now = LocalDateTime.now();
        payload.setCreatedAt(now);
        payload.setUpdatedAt(now);
        return driverRepository.save(payload);
    }

    public Driver update(Long id, Driver payload) {
        Driver existing = driverRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Driver not found"));
        if (payload.getDriverCode() != null && !payload.getDriverCode().isBlank()) {
            existing.setDriverCode(payload.getDriverCode());
        }
        if (payload.getName() != null && !payload.getName().isBlank()) {
            existing.setName(payload.getName());
        }
        existing.setUpdatedAt(LocalDateTime.now());
        return driverRepository.save(existing);
    }

    public void delete(Long id) {
        if (!driverRepository.existsById(id)) {
            throw new NoSuchElementException("Driver not found");
        }
        driverRepository.deleteById(id);
    }
}
