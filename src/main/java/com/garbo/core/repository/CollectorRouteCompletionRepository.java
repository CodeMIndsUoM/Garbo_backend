package com.garbo.core.repository;

import com.garbo.core.entity.CollectorRouteCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CollectorRouteCompletionRepository extends JpaRepository<CollectorRouteCompletion, Long> {

    // =========================
    // 1. TOTAL SUMMARY
    // =========================
    @Query(value = """
        SELECT
            COALESCE(SUM(assigned_bins),  0),
            COALESCE(SUM(collected_bins), 0),
            COALESCE(SUM(missed_bins),    0)
        FROM collector_route_completions
        WHERE completed_at >= :startDate
    """, nativeQuery = true)
    List<Object[]> getSummary(@Param("startDate") LocalDateTime startDate);

    // =========================
    // 2. HOURLY  (DAY filter)
    // Label format: "08:00", "14:00"
    // =========================
    @Query(value = """
        SELECT
            TO_CHAR(completed_at, 'HH24:00') AS label,
            SUM(assigned_bins)               AS assigned,
            SUM(collected_bins)              AS collected,
            SUM(missed_bins)                 AS missed
        FROM collector_route_completions
        WHERE completed_at >= :startDate
        GROUP BY TO_CHAR(completed_at, 'HH24:00')
        ORDER BY TO_CHAR(completed_at, 'HH24:00')
    """, nativeQuery = true)
    List<Object[]> getHourlyData(@Param("startDate") LocalDateTime startDate);

    // =========================
    // 3. DAILY  (WEEK filter)
    // Label format: "Mon", "Tue"
    // =========================
    @Query(value = """
        SELECT
            TO_CHAR(completed_at, 'Dy')  AS label,
            SUM(assigned_bins)           AS assigned,
            SUM(collected_bins)          AS collected,
            SUM(missed_bins)             AS missed
        FROM collector_route_completions
        WHERE completed_at >= :startDate
        GROUP BY TO_CHAR(completed_at, 'Dy'), DATE_TRUNC('day', completed_at)
        ORDER BY MIN(completed_at)
    """, nativeQuery = true)
    List<Object[]> getDailyData(@Param("startDate") LocalDateTime startDate);

    // =========================
    // 4. WEEKLY  (MONTH filter)
    // Label format: "Week 1", "Week 2"
    // =========================
    @Query(value = """
        SELECT
            'Week ' || RANK() OVER (ORDER BY TO_CHAR(completed_at, 'IYYY-IW')) AS label,
            SUM(assigned_bins)  AS assigned,
            SUM(collected_bins) AS collected,
            SUM(missed_bins)    AS missed
        FROM collector_route_completions
        WHERE completed_at >= :startDate
        GROUP BY TO_CHAR(completed_at, 'IYYY-IW')
        ORDER BY TO_CHAR(completed_at, 'IYYY-IW')
    """, nativeQuery = true)
    List<Object[]> getWeeklyData(@Param("startDate") LocalDateTime startDate);
}