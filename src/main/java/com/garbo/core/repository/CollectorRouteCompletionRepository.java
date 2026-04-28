package com.garbo.core.repository;

import com.garbo.core.entity.CollectorRouteCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CollectorRouteCompletionRepository extends JpaRepository<CollectorRouteCompletion, Long> {
    Optional<CollectorRouteCompletion> findByCollectorIdAndSessionId(Long collectorId, String sessionId);

    List<CollectorRouteCompletion> findByCollectorIdOrderByCompletedAtAsc(Long collectorId);

    List<Object[]> getSummary(LocalDateTime startDate);

    List<Object[]> getDailyData(LocalDateTime startDate);

    List<Object[]> getWeeklyData(LocalDateTime startDate);

    List<Object[]> getHourlyData(LocalDateTime startDate);
}
