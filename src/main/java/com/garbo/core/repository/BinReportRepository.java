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

    // ── All councils ───────────────────────────────────────────────────────────

    @Query("SELECT COUNT(r) FROM BinReport r WHERE r.reportedAt >= :start AND r.reportedAt < :end")
    long countReportsBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );

    @Query("SELECT COUNT(DISTINCT r.bin.id) FROM BinReport r WHERE r.reportedAt >= :start AND r.reportedAt < :end")
    long countDistinctBinsBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );

    @Query("SELECT COUNT(DISTINCT r.reporter.empId) FROM BinReport r WHERE r.reportedAt >= :start AND r.reportedAt < :end AND r.reporter IS NOT NULL")
    long countDistinctReportersBetween(
        @Param("start") LocalDateTime start,
        @Param("end")   LocalDateTime end
    );

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

    // ── Filtered by council (via bins.council) ─────────────────────────────────

    @Query(value = """
        SELECT COUNT(r.id)
        FROM bin_reports r
        JOIN bins b ON b.id = r.bin_id
        WHERE r.reported_at >= :start AND r.reported_at < :end
          AND LOWER(b.council) = LOWER(:council)
        """, nativeQuery = true)
    long countReportsBetweenByCouncil(
        @Param("start")   LocalDateTime start,
        @Param("end")     LocalDateTime end,
        @Param("council") String council
    );

    @Query(value = """
        SELECT COUNT(DISTINCT r.bin_id)
        FROM bin_reports r
        JOIN bins b ON b.id = r.bin_id
        WHERE r.reported_at >= :start AND r.reported_at < :end
          AND LOWER(b.council) = LOWER(:council)
        """, nativeQuery = true)
    long countDistinctBinsBetweenByCouncil(
        @Param("start")   LocalDateTime start,
        @Param("end")     LocalDateTime end,
        @Param("council") String council
    );

    @Query(value = """
        SELECT COUNT(DISTINCT r.reporter_id)
        FROM bin_reports r
        JOIN bins b ON b.id = r.bin_id
        WHERE r.reported_at >= :start AND r.reported_at < :end
          AND r.reporter_id IS NOT NULL
          AND LOWER(b.council) = LOWER(:council)
        """, nativeQuery = true)
    long countDistinctReportersBetweenByCouncil(
        @Param("start")   LocalDateTime start,
        @Param("end")     LocalDateTime end,
        @Param("council") String council
    );

    @Query(value = """
        SELECT EXTRACT(HOUR FROM r.reported_at) AS hr, COUNT(r.id)
        FROM bin_reports r
        JOIN bins b ON b.id = r.bin_id
        WHERE r.reported_at >= :start AND r.reported_at < :end
          AND LOWER(b.council) = LOWER(:council)
        GROUP BY EXTRACT(HOUR FROM r.reported_at)
        ORDER BY hr
        """, nativeQuery = true)
    List<Object[]> countByHourBetweenByCouncil(
        @Param("start")   LocalDateTime start,
        @Param("end")     LocalDateTime end,
        @Param("council") String council
    );

    @Query(value = """
        SELECT DATE(r.reported_at) AS day, COUNT(r.id)
        FROM bin_reports r
        JOIN bins b ON b.id = r.bin_id
        WHERE r.reported_at >= :start AND r.reported_at < :end
          AND LOWER(b.council) = LOWER(:council)
        GROUP BY DATE(r.reported_at)
        ORDER BY day
        """, nativeQuery = true)
    List<Object[]> countByDayBetweenByCouncil(
        @Param("start")   LocalDateTime start,
        @Param("end")     LocalDateTime end,
        @Param("council") String council
    );
}