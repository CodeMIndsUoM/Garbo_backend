package com.garbo.core.repository;

import com.garbo.core.entity.RouteAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteAssignmentRepository extends JpaRepository<RouteAssignment, Long> {

    /**
     * Find the assignment for a given session.
     * One session produces exactly one assignment (vehicle + driver + collectors).
     */
    Optional<RouteAssignment> findBySessionId(String sessionId);

    /**
     * Find all assignments that used a specific vehicle.
     * Useful for vehicle utilization reports.
     */
    List<RouteAssignment> findByVehicleId(Long vehicleId);

    /**
     * Find all assignments for a specific driver.
     */
    List<RouteAssignment> findByDriverId(Long driverId);

    /**
     * Find all active assignments for a collector (joined via junction table).
     * "Active" means the parent session has status = 'READY'.
     */
    @Query("""
        SELECT a FROM RouteAssignment a
        JOIN a.collectors c
        JOIN RouteSession s ON s.sessionId = a.sessionId
        WHERE c.empId = :collectorId
          AND s.status = 'READY'
        ORDER BY a.createdAt DESC
    """)
    List<RouteAssignment> findActiveAssignmentsByCollectorId(@Param("collectorId") Long collectorId);

    /**
     * Fetch assignment with all collectors eagerly loaded — avoids N+1 when
     * the caller needs the full team list.
     */
    @Query("""
        SELECT a FROM RouteAssignment a
        LEFT JOIN FETCH a.collectors
        WHERE a.sessionId = :sessionId
    """)
    Optional<RouteAssignment> findBySessionIdWithCollectors(@Param("sessionId") String sessionId);
}