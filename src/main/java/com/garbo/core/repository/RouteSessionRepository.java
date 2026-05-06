package com.garbo.core.repository;


import com.garbo.core.entity.RouteSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface RouteSessionRepository extends JpaRepository<RouteSession, java.util.UUID> {

    /**
     * Find the most recently created session for a given user.
     * Useful for re-attaching to an admin's last active optimization.
     */
    Optional<RouteSession> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all sessions for a user, newest first.
     */
    List<RouteSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all sessions currently in a given status (e.g. "PROCESSING").
     */
    List<RouteSession> findByStatus(String status);

    /**
     * Update the status and version of a session in one query —
     * avoids loading the full entity just for a status bump.
     */
    @Transactional
    @Modifying
    @Query("UPDATE RouteSession s SET s.status = :status, s.version = :version, s.updatedAt = CURRENT_TIMESTAMP WHERE s.sessionId = :sessionId")
    int updateStatus(
        @Param("sessionId") java.util.UUID sessionId,
        @Param("status") String status,
        @Param("version") Long version
    );
}