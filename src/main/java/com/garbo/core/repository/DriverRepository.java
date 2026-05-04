package com.garbo.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garbo.core.entity.Driver;

import java.util.List;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    Optional<Driver> findByDriverCodeIgnoreCase(String driverCode);
    List<Driver> findByCouncil(String council);
}
