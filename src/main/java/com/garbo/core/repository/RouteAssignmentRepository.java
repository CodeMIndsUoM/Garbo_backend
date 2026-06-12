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

    Optional<RouteAssignment> findBySessionId(java.util.UUID sessionId);

    List<RouteAssignment> findByVehicleId(Long vehicleId);

    List<RouteAssignment> findByDriverEmpId(Long empId);

    @Query("""
        SELECT a FROM RouteAssignment a
        JOIN a.collectors c
        JOIN RouteSession s ON s.sessionId = a.sessionId
        WHERE c.id = :collectorId
          AND s.status = 'READY'
        ORDER BY a.createdAt DESC
    """)
    List<RouteAssignment> findActiveAssignmentsByCollectorId(@Param("collectorId") Long collectorId);

    @Query("""
        SELECT a FROM RouteAssignment a
        LEFT JOIN FETCH a.collectors
        WHERE a.sessionId = :sessionId
    """)
    Optional<RouteAssignment> findBySessionIdWithCollectors(@Param("sessionId") java.util.UUID sessionId);

    @Query("""
        SELECT a.vehicle.id FROM RouteAssignment a
        JOIN RouteSession s ON s.sessionId = a.sessionId
        WHERE s.status IN ('READY', 'PROCESSING')
    """)
    List<Long> findBusyVehicleIds();

    @Query("""
        SELECT a.driver.empId FROM RouteAssignment a
        JOIN RouteSession s ON s.sessionId = a.sessionId
        WHERE s.status IN ('READY', 'PROCESSING')
    """)
    List<Long> findBusyDriverIds();

    @Query("""
        SELECT c.id FROM RouteAssignment a
        JOIN a.collectors c
        JOIN RouteSession s ON s.sessionId = a.sessionId
        WHERE s.status IN ('READY', 'PROCESSING')
    """)
    List<Long> findBusyCollectorIds();

    @Query("""
        SELECT a FROM RouteAssignment a
        JOIN FETCH a.vehicle
        JOIN RouteSession s ON s.sessionId = a.sessionId
        WHERE s.userId = :userId
          AND s.status IN ('READY', 'PROCESSING')
        ORDER BY a.createdAt DESC
    """)
    List<RouteAssignment> findActiveByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT a, s.status FROM RouteAssignment a
        JOIN FETCH a.vehicle
        JOIN FETCH a.driver
        JOIN RouteSession s ON s.sessionId = a.sessionId
        ORDER BY a.createdAt DESC
    """)
    List<Object[]> findAllWithStatus();

    @Query("""
        SELECT a, s.status FROM RouteAssignment a
        JOIN FETCH a.vehicle
        JOIN FETCH a.driver
        JOIN RouteSession s ON s.sessionId = a.sessionId
        WHERE LOWER(a.vehicle.assignedCouncil) = LOWER(:council)
           OR LOWER(a.driver.assignedCouncil) = LOWER(:council)
        ORDER BY a.createdAt DESC
    """)
    List<Object[]> findAllByCouncilWithStatus(@Param("council") String council);

    @Query("""
        SELECT a, s.status FROM RouteAssignment a
        JOIN FETCH a.vehicle
        JOIN FETCH a.driver
        JOIN RouteSession s ON s.sessionId = a.sessionId
        WHERE s.userId = :userId OR a.driver.empId = :userId
        ORDER BY a.createdAt DESC
    """)
    List<Object[]> findAllByUserIdWithStatus(@Param("userId") Long userId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByVehicleId(Long vehicleId);

    @org.springframework.transaction.annotation.Transactional
    void deleteByDriverEmpId(Long empId);
}