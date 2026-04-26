package com.garbo.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.garbo.core.entity.WasteBin;

public interface WasteBinRepository extends JpaRepository<WasteBin, String> {
    Optional<WasteBin> findByIdIgnoreCase(String id);
}
