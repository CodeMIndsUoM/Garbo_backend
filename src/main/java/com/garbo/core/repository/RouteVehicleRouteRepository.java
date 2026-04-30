package com.garbo.core.repository;

import com.garbo.core.entity.RouteVehicleRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteVehicleRouteRepository extends JpaRepository<RouteVehicleRoute, Long> {

    /**
     * Find all vehicle routes for a session, ordered by vehicleKey.
     * Mirrors the Map<Integer, VehicleRoute> structure from RouteResponseDTO.
     */
    List<RouteVehicleRoute> findBySessionIdOrderByVehicleKeyAsc(String sessionId);

    /**
     * Find vehicle routes for a session with their bin stops eagerly loaded.
     * Use this when you need the full route + stop list in one query (e.g. for
     * broadcasting the route to a collector's device).
     */
    @Query("""
        SELECT vr FROM RouteVehicleRoute vr
        LEFT JOIN FETCH vr.binStops bs
        WHERE vr.sessionId = :sessionId
        ORDER BY vr.vehicleKey ASC, bs.stopOrder ASC
    """)
    List<RouteVehicleRoute> findBySessionIdWithStops(@Param("sessionId") String sessionId);

    /**
     * Delete all vehicle routes (and their stops via cascade) for a session.
     * Called before re-persisting a recomputed route so old data is replaced cleanly.
     */
    void deleteBySessionId(String sessionId);
}