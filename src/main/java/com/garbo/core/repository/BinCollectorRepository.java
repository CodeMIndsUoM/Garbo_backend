package com.garbo.core.repository;

import com.garbo.core.entity.BinCollector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinCollectorRepository extends JpaRepository<BinCollector, Long> {
    List<BinCollector> findByAssignedCouncil(String assignedCouncil);
}
