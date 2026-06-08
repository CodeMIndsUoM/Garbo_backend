package com.garbo.core.repository;

import com.garbo.core.entity.RouteBinStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ZoneCollectionRepository extends JpaRepository<RouteBinStop, Long> {

    // ── All councils ───────────────────────────────────────────────────────────

    @Query(value = """
            SELECT b.zone AS zone, COUNT(r.id) AS collected
            FROM route_bin_stops r
            JOIN bins b ON b.id = r.bin_id
            WHERE r.collected_at IS NOT NULL
              AND r.collected_at >= :startDate
            GROUP BY b.zone
            ORDER BY b.zone
            """, nativeQuery = true)
    List<Object[]> getCollectedByZone(@Param("startDate") LocalDateTime startDate);

    // ── Filtered by council ────────────────────────────────────────────────────

    @Query(value = """
            SELECT b.zone AS zone, COUNT(r.id) AS collected
            FROM route_bin_stops r
            JOIN bins b ON b.id = r.bin_id
            WHERE r.collected_at IS NOT NULL
              AND r.collected_at >= :startDate
              AND LOWER(b.council) = LOWER(:council)
            GROUP BY b.zone
            ORDER BY b.zone
            """, nativeQuery = true)
    List<Object[]> getCollectedByZoneAndCouncil(
            @Param("startDate") LocalDateTime startDate,
            @Param("council") String council
    );
}