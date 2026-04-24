package com.garbo.core.repository;

import com.garbo.core.entity.ScoreTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScoreTransactionRepository extends JpaRepository<ScoreTransaction, Long> {

    interface TaskScoreAggregate {
        Long getUserId();

        String getRole();

        Double getTotalPoints();
    }

    boolean existsByUserIdAndTaskIdAndSourceEventId(Long userId, Long taskId, String sourceEventId);

    boolean existsByUserIdAndTaskIdAndPeriodKey(Long userId, Long taskId, String periodKey);

    List<ScoreTransaction> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    List<ScoreTransaction> findByUserIdAndTaskIsNotNullOrderByCreatedAtDesc(Long userId);

        @Query(value = """
             SELECT dedup.user_id AS userId,
                 dedup.role AS role,
                 COALESCE(SUM(dedup.max_points), 0) AS totalPoints
             FROM (
              SELECT st.user_id,
                  st.role,
                  st.task_id,
                  st.period_key,
                  MAX(st.points_delta) AS max_points
              FROM score_transactions st
              WHERE st.task_id IS NOT NULL
              GROUP BY st.user_id, st.role, st.task_id, st.period_key
             ) dedup
             GROUP BY dedup.user_id, dedup.role
             """, nativeQuery = true)
    List<TaskScoreAggregate> findTaskScoreAggregates();
}
