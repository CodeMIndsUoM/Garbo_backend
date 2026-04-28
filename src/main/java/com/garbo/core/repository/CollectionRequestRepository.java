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

    List<Object[]> countBySlotGroupedAllTime();

    List<Object[]> countBySlotGrouped(Instant from);

    long countByCreatedAtAfter(Instant from);

    long countCompletedAfter(Instant from);

    List<Object[]> countByWasteTypeGrouped(Instant from);

    List<Object[]> countByStatusGroupedAllTime();

    List<Object[]> countByStatusGrouped(Instant from);

    List<Object[]> countByWasteTypeGroupedAllTime();

    long countCompletedAllTime();
}
