package com.garbo.core.repository;

import com.garbo.core.entity.BinCollector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BinCollectorRepository extends JpaRepository<BinCollector, Long> {

    // ── Basic finders ─────────────────────────────────────────────────────────

    List<BinCollector> findByAssignedCouncil(String assignedCouncil);

    // ── 1. SUMMARY — all councils ─────────────────────────────────────────────

    @Query(value = """
        SELECT
            COUNT(*)                                        AS total,
            COUNT(*) FILTER (WHERE on_duty = true)          AS on_duty,
            COUNT(*) FILTER (WHERE on_duty = false)         AS on_leave,
            COALESCE(
                AVG(
                    CASE
                        WHEN (completed_collections + missed_collections) > 0
                        THEN (CAST(completed_collections AS float) /
                             (completed_collections + missed_collections)) * 100
                        ELSE NULL
                    END
                ), 0
            )                                               AS avg_performance
        FROM bin_collectors
    """, nativeQuery = true)
    List<Object[]> getSummary();

    // ── 2. ZONE BREAKDOWN — all councils ─────────────────────────────────────

    @Query(value = """
        SELECT
            assigned_zone AS zone,
            COUNT(*)      AS staff,
            COALESCE(
                AVG(
                    CASE
                        WHEN (completed_collections + missed_collections) > 0
                        THEN (CAST(completed_collections AS float) /
                             (completed_collections + missed_collections)) * 100
                        ELSE NULL
                    END
                ), 0
            )             AS performance
        FROM bin_collectors
        WHERE assigned_zone IS NOT NULL
        GROUP BY assigned_zone
        ORDER BY assigned_zone
    """, nativeQuery = true)
    List<Object[]> getZoneBreakdown();

    // ── 3. SUMMARY — filtered by council ─────────────────────────────────────

    @Query(value = """
        SELECT
            COUNT(*)                                        AS total,
            COUNT(*) FILTER (WHERE on_duty = true)          AS on_duty,
            COUNT(*) FILTER (WHERE on_duty = false)         AS on_leave,
            COALESCE(
                AVG(
                    CASE
                        WHEN (completed_collections + missed_collections) > 0
                        THEN (CAST(completed_collections AS float) /
                             (completed_collections + missed_collections)) * 100
                        ELSE NULL
                    END
                ), 0
            )                                               AS avg_performance
        FROM bin_collectors
        WHERE LOWER(assigned_council) = LOWER(:council)
    """, nativeQuery = true)
    List<Object[]> getSummaryByCouncil(@Param("council") String council);

    // ── 4. ZONE BREAKDOWN — filtered by council ───────────────────────────────

    @Query(value = """
        SELECT
            assigned_zone AS zone,
            COUNT(*)      AS staff,
            COALESCE(
                AVG(
                    CASE
                        WHEN (completed_collections + missed_collections) > 0
                        THEN (CAST(completed_collections AS float) /
                             (completed_collections + missed_collections)) * 100
                        ELSE NULL
                    END
                ), 0
            )             AS performance
        FROM bin_collectors
        WHERE assigned_zone IS NOT NULL
          AND LOWER(assigned_council) = LOWER(:council)
        GROUP BY assigned_zone
        ORDER BY assigned_zone
    """, nativeQuery = true)
    List<Object[]> getZoneBreakdownByCouncil(@Param("council") String council);
}