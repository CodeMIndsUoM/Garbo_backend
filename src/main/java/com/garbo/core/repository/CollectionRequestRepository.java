package com.garbo.core.repository;

<<<<<<< HEAD
import com.garbo.core.entity.CollectionRequest;
import com.garbo.core.enums.RequestStatus;
=======

import com.garbo.core.entity.CollectionRequest;
>>>>>>> kevin-RWS
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

<<<<<<< HEAD
=======
import java.time.Instant;
>>>>>>> kevin-RWS
import java.util.List;

@Repository
public interface CollectionRequestRepository extends JpaRepository<CollectionRequest, Long> {

<<<<<<< HEAD
    List<CollectionRequest> findByCitizen_EmpIdOrderByCreatedAtDesc(Long citizenId);

    List<CollectionRequest> findByCitizen_EmpIdAndStatusOrderByCreatedAtDesc(Long citizenId, RequestStatus status);

    long countByStatus(RequestStatus status);

    List<CollectionRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /**
     * OPEN requests sorted by Haversine distance from (lat, lng) when provided.
     * Falls back to creation time when coordinates are null.
     */
    @Query(value = """
            SELECT r FROM CollectionRequest r
            WHERE r.status = com.garbo.core.enums.RequestStatus.OPEN
            ORDER BY
              CASE WHEN :lat IS NULL OR :lng IS NULL THEN 0 ELSE
                (6371 * acos(
                  cos(radians(:lat)) * cos(radians(r.latitude)) *
                  cos(radians(r.longitude) - radians(:lng)) +
                  sin(radians(:lat)) * sin(radians(r.latitude))
                ))
              END ASC,
              r.createdAt DESC
            """)
    List<CollectionRequest> findOpenFeedNear(@Param("lat") Double lat, @Param("lng") Double lng);
}
=======
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
>>>>>>> kevin-RWS
