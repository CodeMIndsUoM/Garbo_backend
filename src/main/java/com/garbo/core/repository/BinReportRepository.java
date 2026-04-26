package com.garbo.core.repository;

import com.garbo.core.entity.BinReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BinReportRepository extends JpaRepository<BinReport, Long> {
}
