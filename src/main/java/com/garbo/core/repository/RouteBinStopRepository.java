package com.garbo.core.repository;

import com.garbo.core.entity.RouteBinStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RouteBinStopRepository extends JpaRepository<RouteBinStop, Long> {

    /**
     * Find all stops for a vehicle route, in collection order.
     */
    List<RouteBinStop> findByVehicleRouteIdOrderByStopOrderAsc(Long vehicleRouteId);

    /**
     * Find a specific stop by its parent route and bin ID.
     * Used when a BIN_COLLECTED WebSocket message arrives — look up the stop
     * to mark it collected without loading the full route.
     */
    @Query("""
        SELECT s FROM RouteBinStop s
        WHERE s.vehicleRoute.id = :vehicleRouteId
          AND s.binId = :binId
    """)
    Optional<RouteBinStop> findByVehicleRouteIdAndBinId(
        @Param("vehicleRouteId") Long vehicleRouteId,
        @Param("binId") Long binId
    );

    /**
     * Find a stop by session (via join) and bin ID.
     * Handy when only the sessionId and binId are known (no vehicleRouteId).
     */
    @Query("""
        SELECT s FROM RouteBinStop s
        JOIN s.vehicleRoute vr
        WHERE vr.sessionId = :sessionId
          AND s.binId = :binId
    """)
    Optional<RouteBinStop> findBySessionIdAndBinId(
        @Param("sessionId") java.util.UUID sessionId,
        @Param("binId") Long binId
    );

    /**
     * Mark a single bin stop as COLLECTED.
     * Called by BinCollectionRealtimeService when a collector scans a bin.
     */
    @Transactional
    @Modifying
    @Query("""
        UPDATE RouteBinStop s
        SET s.status = 'COLLECTED', s.collectedAt = :collectedAt
        WHERE s.id = :stopId
          AND s.status = 'PENDING'
    """)
    int markCollected(
        @Param("stopId") Long stopId,
        @Param("collectedAt") LocalDateTime collectedAt
    );

    /**
     * Mark a bin stop as SKIPPED.
     */
    @Transactional
    @Modifying
    @Query("""
        UPDATE RouteBinStop s
        SET s.status = 'SKIPPED'
        WHERE s.id = :stopId
          AND s.status = 'PENDING'
    """)
    int markSkipped(@Param("stopId") Long stopId);

    /**
     * Count stops by status for a given session — used for progress tracking.
     * Returns a List of Object[]{status, count}.
     */
    @Query("""
        SELECT s.status, COUNT(s)
        FROM RouteBinStop s
        JOIN s.vehicleRoute vr
        WHERE vr.sessionId = :sessionId
        GROUP BY s.status
    """)
    List<Object[]> countByStatusForSession(@Param("sessionId") java.util.UUID sessionId);

    /**
     * Find all PENDING stops across all vehicle routes of a session.
     * Useful for broadcasting remaining work to the collector team.
     */
    @Query("""
        SELECT s FROM RouteBinStop s
        JOIN s.vehicleRoute vr
        WHERE vr.sessionId = :sessionId
          AND s.status = 'PENDING'
        ORDER BY s.vehicleRoute.vehicleKey ASC, s.stopOrder ASC
    """)
    List<RouteBinStop> findPendingBySessionId(@Param("sessionId") java.util.UUID sessionId);

    @Transactional
    @Modifying
    @Query("DELETE FROM RouteBinStop s WHERE s.vehicleRoute.id IN (SELECT vr.id FROM RouteVehicleRoute vr WHERE vr.sessionId = :sessionId)")
    void deleteBySessionId(@Param("sessionId") java.util.UUID sessionId);
}