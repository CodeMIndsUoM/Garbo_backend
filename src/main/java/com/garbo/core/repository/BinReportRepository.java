// BinReportRepository.java
package com.garbo.core.repository;

import com.garbo.core.entity.BinReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BinReportRepository extends JpaRepository<BinReport, Long> {

    //  KPI: Total reports today 
    @Query("SELECT COUNT(r) FROM BinReport r WHERE r.reportedAt >= :start AND r.reportedAt < :end")
    long countReportsBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );

    //  KPI: Distinct bins affected today 
    @Query("SELECT COUNT(DISTINCT r.bin.id) FROM BinReport r WHERE r.reportedAt >= :start AND r.reportedAt < :end")
    long countDistinctBinsBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );

    //  KPI: Distinct reporters today (non-null reporter_id only) 
    @Query("SELECT COUNT(DISTINCT r.reporter.empId) FROM BinReport r WHERE r.reportedAt >= :start AND r.reportedAt < :end AND r.reporter IS NOT NULL")
    long countDistinctReportersBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );

    //  Chart: Hourly counts for today (PostgreSQL: EXTRACT instead of HOUR())
    // Returns Object[] { hour (Double in PG), count (Long) }
    @Query(value = """
        SELECT EXTRACT(HOUR FROM reported_at) AS hr, COUNT(id)
        FROM bin_reports
        WHERE reported_at >= :start AND reported_at < :end
        GROUP BY EXTRACT(HOUR FROM reported_at)
        ORDER BY hr
        """, nativeQuery = true)
    List<Object[]> countByHourBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );

    // Chart: Daily counts for last 7 days (PostgreSQL: DATE() cast) 
    // Returns Object[] { date (java.sql.Date), count (Long) }
    @Query(value = """
        SELECT DATE(reported_at) AS day, COUNT(id)
        FROM bin_reports
        WHERE reported_at >= :start AND reported_at < :end
        GROUP BY DATE(reported_at)
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> countByDayBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );
}