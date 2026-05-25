package com.garbo.core.repository;

import com.garbo.core.entity.CollectionRequest;
import com.garbo.core.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CollectionRequestRepository extends JpaRepository<CollectionRequest, Long> {

    List<CollectionRequest> findByCitizen_EmpIdOrderByCreatedAtDesc(Long citizenId);

    List<CollectionRequest> findByCitizen_EmpIdAndStatusOrderByCreatedAtDesc(Long citizenId, RequestStatus status);

    long countByStatus(RequestStatus status);

    List<CollectionRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

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

    // ── All councils — total count ────────────────────────────────────────────

    @Query("SELECT COUNT(r) FROM CollectionRequest r WHERE r.createdAt >= :from")
    long countByCreatedAtAfter(@Param("from") Instant from);

    // ── All councils — status ─────────────────────────────────────────────────

    @Query("""
        SELECT r.status, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
        GROUP BY r.status
    """)
    List<Object[]> countByStatusGrouped(@Param("from") Instant from);

    @Query("SELECT r.status, COUNT(r) FROM CollectionRequest r GROUP BY r.status")
    List<Object[]> countByStatusGroupedAllTime();

    // ── All councils — slot ───────────────────────────────────────────────────

    @Query("""
        SELECT r.preferredSlot, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
        GROUP BY r.preferredSlot
    """)
    List<Object[]> countBySlotGrouped(@Param("from") Instant from);

    @Query("SELECT r.preferredSlot, COUNT(r) FROM CollectionRequest r GROUP BY r.preferredSlot")
    List<Object[]> countBySlotGroupedAllTime();

    // ── All councils — waste type ─────────────────────────────────────────────

    @Query("""
        SELECT r.wasteType, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
        GROUP BY r.wasteType
    """)
    List<Object[]> countByWasteTypeGrouped(@Param("from") Instant from);

    @Query("SELECT r.wasteType, COUNT(r) FROM CollectionRequest r GROUP BY r.wasteType")
    List<Object[]> countByWasteTypeGroupedAllTime();

    // ── All councils — completed ──────────────────────────────────────────────

    @Query("""
        SELECT COUNT(r)
        FROM CollectionRequest r
        WHERE r.status = com.garbo.core.enums.RequestStatus.COMPLETED
          AND r.createdAt >= :from
    """)
    long countCompletedAfter(@Param("from") Instant from);

    @Query("SELECT COUNT(r) FROM CollectionRequest r WHERE r.status = com.garbo.core.enums.RequestStatus.COMPLETED")
    long countCompletedAllTime();

    // ── Council filtered — total count ────────────────────────────────────────

    @Query("SELECT COUNT(r) FROM CollectionRequest r WHERE r.createdAt >= :from AND LOWER(r.council) = LOWER(:council)")
    long countByCreatedAtAfterAndCouncil(@Param("from") Instant from, @Param("council") String council);

    @Query("SELECT COUNT(r) FROM CollectionRequest r WHERE LOWER(r.council) = LOWER(:council)")
    long countByCouncil(@Param("council") String council);

    // ── Council filtered — status ─────────────────────────────────────────────

    @Query("""
        SELECT r.status, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
          AND LOWER(r.council) = LOWER(:council)
        GROUP BY r.status
    """)
    List<Object[]> countByStatusGroupedAndCouncil(@Param("from") Instant from, @Param("council") String council);

    @Query("""
        SELECT r.status, COUNT(r)
        FROM CollectionRequest r
        WHERE LOWER(r.council) = LOWER(:council)
        GROUP BY r.status
    """)
    List<Object[]> countByStatusGroupedAllTimeAndCouncil(@Param("council") String council);

    // ── Council filtered — slot ───────────────────────────────────────────────

    @Query("""
        SELECT r.preferredSlot, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
          AND LOWER(r.council) = LOWER(:council)
        GROUP BY r.preferredSlot
    """)
    List<Object[]> countBySlotGroupedAndCouncil(@Param("from") Instant from, @Param("council") String council);

    @Query("""
        SELECT r.preferredSlot, COUNT(r)
        FROM CollectionRequest r
        WHERE LOWER(r.council) = LOWER(:council)
        GROUP BY r.preferredSlot
    """)
    List<Object[]> countBySlotGroupedAllTimeAndCouncil(@Param("council") String council);

    // ── Council filtered — waste type ─────────────────────────────────────────

    @Query("""
        SELECT r.wasteType, COUNT(r)
        FROM CollectionRequest r
        WHERE r.createdAt >= :from
          AND LOWER(r.council) = LOWER(:council)
        GROUP BY r.wasteType
    """)
    List<Object[]> countByWasteTypeGroupedAndCouncil(@Param("from") Instant from, @Param("council") String council);

    @Query("""
        SELECT r.wasteType, COUNT(r)
        FROM CollectionRequest r
        WHERE LOWER(r.council) = LOWER(:council)
        GROUP BY r.wasteType
    """)
    List<Object[]> countByWasteTypeGroupedAllTimeAndCouncil(@Param("council") String council);

    // ── Council filtered — completed ──────────────────────────────────────────

    @Query("""
        SELECT COUNT(r)
        FROM CollectionRequest r
        WHERE r.status = com.garbo.core.enums.RequestStatus.COMPLETED
          AND r.createdAt >= :from
          AND LOWER(r.council) = LOWER(:council)
    """)
    long countCompletedAfterAndCouncil(@Param("from") Instant from, @Param("council") String council);

    @Query("""
        SELECT COUNT(r)
        FROM CollectionRequest r
        WHERE r.status = com.garbo.core.enums.RequestStatus.COMPLETED
          AND LOWER(r.council) = LOWER(:council)
    """)
    long countCompletedAllTimeAndCouncil(@Param("council") String council);
}