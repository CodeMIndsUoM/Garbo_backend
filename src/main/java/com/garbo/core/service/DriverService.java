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

    public List<Driver> getByCouncil(String council) {
        return driverRepository.findByCouncil(council);
    }

    public Driver create(Driver payload) {
        System.out.println("DriverService: Creating driver: " + payload.getName() + ", council: " + payload.getCouncil());
        if (payload.getName() == null || payload.getName().isBlank()) {
            throw new IllegalArgumentException("Driver name is required");
        }
        
        String council = payload.getCouncil();
        if (council == null || council.isBlank()) {
            council = "Unassigned";
            payload.setCouncil(council);
        }
        
        List<Driver> existingDrivers = driverRepository.findByCouncil(council);
        System.out.println("DriverService: Found " + existingDrivers.size() + " existing drivers for council " + council);
        int max = 0;
        for (Driver d : existingDrivers) {
            if (d.getDriverCode() != null && d.getDriverCode().startsWith("DRV-")) {
                try {
                    int num = Integer.parseInt(d.getDriverCode().substring(4));
                    max = Math.max(max, num);
                } catch (NumberFormatException ignored) {}
            }
        }
        payload.setDriverCode("DRV-" + (max + 1));
        System.out.println("DriverService: Assigned driver code " + payload.getDriverCode());

        LocalDateTime now = LocalDateTime.now();
        payload.setCreatedAt(now);
        payload.setUpdatedAt(now);
        Driver saved = driverRepository.save(payload);
        System.out.println("DriverService: Saved driver with ID " + saved.getId());
        return saved;
    }

    public Driver update(Long id, Driver payload) {
        Driver existing = driverRepository.findById(id).orElseThrow(() -> new NoSuchElementException("Driver not found"));
        if (payload.getDriverCode() != null && !payload.getDriverCode().isBlank()) {
            existing.setDriverCode(payload.getDriverCode());
        }
        if (payload.getName() != null && !payload.getName().isBlank()) {
            existing.setName(payload.getName());
        }
        if (payload.getCouncil() != null) {
            existing.setCouncil(payload.getCouncil());
        }
        if (payload.getEmail() != null) {
            existing.setEmail(payload.getEmail());
        }
        if (payload.getPhone() != null) {
            existing.setPhone(payload.getPhone());
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
