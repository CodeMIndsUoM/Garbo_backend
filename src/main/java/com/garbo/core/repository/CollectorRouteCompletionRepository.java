package com.garbo.core.repository;

import com.garbo.core.entity.CollectorRouteCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectorRouteCompletionRepository extends JpaRepository<CollectorRouteCompletion, Long> {

    Optional<CollectorRouteCompletion> findByCollectorIdAndSessionId(Long collectorId, String sessionId);

    List<CollectorRouteCompletion> findByCollectorIdOrderByCompletedAtAsc(Long collectorId);

    // ── All councils ───────────────────────────────────────────────────────────

    @Query(value = """
            SELECT
                COALESCE(SUM(assigned_bins), 0) AS assigned,
                COALESCE(SUM(collected_bins), 0) AS collected,
                COALESCE(SUM(missed_bins), 0) AS missed
            FROM collector_route_completions
            WHERE completed_at >= :startDate
            """, nativeQuery = true)
    List<Object[]> getSummary(@Param("startDate") LocalDateTime startDate);

    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('hour', completed_at), 'YYYY-MM-DD HH24:00') AS label,
                COALESCE(SUM(assigned_bins), 0) AS assigned,
                COALESCE(SUM(collected_bins), 0) AS collected,
                COALESCE(SUM(missed_bins), 0) AS missed
            FROM collector_route_completions
            WHERE completed_at >= :startDate
            GROUP BY DATE_TRUNC('hour', completed_at)
            ORDER BY DATE_TRUNC('hour', completed_at)
            """, nativeQuery = true)
    List<Object[]> getHourlyData(@Param("startDate") LocalDateTime startDate);

    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('day', completed_at), 'YYYY-MM-DD') AS label,
                COALESCE(SUM(assigned_bins), 0) AS assigned,
                COALESCE(SUM(collected_bins), 0) AS collected,
                COALESCE(SUM(missed_bins), 0) AS missed
            FROM collector_route_completions
            WHERE completed_at >= :startDate
            GROUP BY DATE_TRUNC('day', completed_at)
            ORDER BY DATE_TRUNC('day', completed_at)
            """, nativeQuery = true)
    List<Object[]> getDailyData(@Param("startDate") LocalDateTime startDate);

    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('week', completed_at), 'IYYY-"W"IW') AS label,
                COALESCE(SUM(assigned_bins), 0) AS assigned,
                COALESCE(SUM(collected_bins), 0) AS collected,
                COALESCE(SUM(missed_bins), 0) AS missed
            FROM collector_route_completions
            WHERE completed_at >= :startDate
            GROUP BY DATE_TRUNC('week', completed_at)
            ORDER BY DATE_TRUNC('week', completed_at)
            """, nativeQuery = true)
    List<Object[]> getWeeklyData(@Param("startDate") LocalDateTime startDate);

    // ── Filtered by council (joins bin_collectors) ─────────────────────────────

    @Query(value = """
            SELECT
                COALESCE(SUM(r.assigned_bins), 0) AS assigned,
                COALESCE(SUM(r.collected_bins), 0) AS collected,
                COALESCE(SUM(r.missed_bins), 0) AS missed
            FROM collector_route_completions r
            JOIN bin_collectors bc ON bc.emp_id = r.collector_id
            WHERE r.completed_at >= :startDate
              AND LOWER(bc.assigned_council) = LOWER(:council)
            """, nativeQuery = true)
    List<Object[]> getSummaryByCouncil(@Param("startDate") LocalDateTime startDate,
                                        @Param("council") String council);

    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('hour', r.completed_at), 'YYYY-MM-DD HH24:00') AS label,
                COALESCE(SUM(r.assigned_bins), 0) AS assigned,
                COALESCE(SUM(r.collected_bins), 0) AS collected,
                COALESCE(SUM(r.missed_bins), 0) AS missed
            FROM collector_route_completions r
            JOIN bin_collectors bc ON bc.emp_id = r.collector_id
            WHERE r.completed_at >= :startDate
              AND LOWER(bc.assigned_council) = LOWER(:council)
            GROUP BY DATE_TRUNC('hour', r.completed_at)
            ORDER BY DATE_TRUNC('hour', r.completed_at)
            """, nativeQuery = true)
    List<Object[]> getHourlyDataByCouncil(@Param("startDate") LocalDateTime startDate,
                                           @Param("council") String council);

    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('day', r.completed_at), 'YYYY-MM-DD') AS label,
                COALESCE(SUM(r.assigned_bins), 0) AS assigned,
                COALESCE(SUM(r.collected_bins), 0) AS collected,
                COALESCE(SUM(r.missed_bins), 0) AS missed
            FROM collector_route_completions r
            JOIN bin_collectors bc ON bc.emp_id = r.collector_id
            WHERE r.completed_at >= :startDate
              AND LOWER(bc.assigned_council) = LOWER(:council)
            GROUP BY DATE_TRUNC('day', r.completed_at)
            ORDER BY DATE_TRUNC('day', r.completed_at)
            """, nativeQuery = true)
    List<Object[]> getDailyDataByCouncil(@Param("startDate") LocalDateTime startDate,
                                          @Param("council") String council);

    @Query(value = """
            SELECT
                TO_CHAR(DATE_TRUNC('week', r.completed_at), 'IYYY-"W"IW') AS label,
                COALESCE(SUM(r.assigned_bins), 0) AS assigned,
                COALESCE(SUM(r.collected_bins), 0) AS collected,
                COALESCE(SUM(r.missed_bins), 0) AS missed
            FROM collector_route_completions r
            JOIN bin_collectors bc ON bc.emp_id = r.collector_id
            WHERE r.completed_at >= :startDate
              AND LOWER(bc.assigned_council) = LOWER(:council)
            GROUP BY DATE_TRUNC('week', r.completed_at)
            ORDER BY DATE_TRUNC('week', r.completed_at)
            """, nativeQuery = true)
    List<Object[]> getWeeklyDataByCouncil(@Param("startDate") LocalDateTime startDate,
                                           @Param("council") String council);
}