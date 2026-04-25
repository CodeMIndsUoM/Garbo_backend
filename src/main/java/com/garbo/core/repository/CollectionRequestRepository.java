package com.garbo.core.repository;


import com.garbo.core.entity.CollectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CollectionRequestRepository extends JpaRepository<CollectionRequest, Long> {

    // ── Total count within a time window ─────────────────────────────────────
    @Query("SELECT COUNT(r) FROM CollectionRequest r WHERE r.createdAt >= :from")
    long countByCreatedAtAfter(@Param("from") Instant from);

    // All-time count (no filter)
    long count();

    // ── Count by status within window ─────────────────────────────────────────
    @Query("""
        SELECT r.status, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
        GROUP BY r.status
    """)
    List<Object[]> countByStatusGrouped(@Param("from") Instant from);

    // All-time status distribution
    @Query("SELECT r.status, COUNT(r) FROM CollectionRequest r GROUP BY r.status")
    List<Object[]> countByStatusGroupedAllTime();

    // ── Count by preferred slot within window ─────────────────────────────────
    @Query("""
        SELECT r.preferredSlot, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
        GROUP BY r.preferredSlot
    """)
    List<Object[]> countBySlotGrouped(@Param("from") Instant from);

    @Query("SELECT r.preferredSlot, COUNT(r) FROM CollectionRequest r GROUP BY r.preferredSlot")
    List<Object[]> countBySlotGroupedAllTime();

    // ── Count by waste type within window ─────────────────────────────────────
    @Query("""
        SELECT r.wasteType, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
        GROUP BY r.wasteType
    """)
    List<Object[]> countByWasteTypeGrouped(@Param("from") Instant from);

    @Query("SELECT r.wasteType, COUNT(r) FROM CollectionRequest r GROUP BY r.wasteType")
    List<Object[]> countByWasteTypeGroupedAllTime();

    // ── Completed count within window (for completion-rate calculation) ────────
    @Query("""
        SELECT COUNT(r)
        FROM CollectionRequest r
        WHERE r.status = 'COMPLETED'
          AND r.createdAt >= :from
    """)
    long countCompletedAfter(@Param("from") Instant from);

    @Query("SELECT COUNT(r) FROM CollectionRequest r WHERE r.status = 'COMPLETED'")
    long countCompletedAllTime();
}