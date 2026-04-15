package com.garbo.core.repository;

import com.garbo.core.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {
    boolean existsByDriverCode(String driverCode);

    boolean existsByDriverCodeAndIdNot(String driverCode, Long id);
}
