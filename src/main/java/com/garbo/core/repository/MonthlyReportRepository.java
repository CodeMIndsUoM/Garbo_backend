package com.garbo.core.repository;


import com.garbo.core.entity.MonthlyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReport, Long> {

    /**
     * All reports ordered by newest first — used for the reports list page.
     */
    List<MonthlyReport> findAllByOrderByCreatedAtDesc();

    /**
     * Check if a report already exists for a given period_start
     * so we don't generate duplicates for the same month.
     */
    Optional<MonthlyReport> findByPeriodStart(LocalDate periodStart);

    /**
     * Find reports by status (COMPLETED / PROCESSING / FAILED).
     */
    List<MonthlyReport> findByStatusOrderByCreatedAtDesc(String status);

    /**
     * Count how many reports have been generated this calendar year.
     */
    @Query("SELECT COUNT(r) FROM MonthlyReport r WHERE YEAR(r.createdAt) = :year")
    long countByYear(int year);
}