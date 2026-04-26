package com.garbo.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garbo.core.entity.Driver;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByDriverCodeIgnoreCase(String driverCode);
}
