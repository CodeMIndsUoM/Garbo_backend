package com.garbo.core.repository;

import com.garbo.core.entity.UserTaskProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTaskProgressRepository extends JpaRepository<UserTaskProgress, Long> {

    interface CompletedPointsAggregate {
        Long getUserId();

        Double getTotalPoints();
    }

    Optional<UserTaskProgress> findByUserIdAndTaskId(Long userId, Long taskId);

    List<UserTaskProgress> findByUserId(Long userId);

    @Query("""
            SELECT p.userId AS userId,
                   COALESCE(SUM(p.pointsEarned), 0) AS totalPoints
            FROM UserTaskProgress p
            WHERE p.isCompleted = true
            GROUP BY p.userId
            """)
    List<CompletedPointsAggregate> findCompletedPointsByUser();
}
