package com.garbo.core.repository;

import com.garbo.core.entity.Bin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinRepository extends JpaRepository<Bin, Long> {
    List<Bin> findByAssignedToEmpId(Long empId);
}
