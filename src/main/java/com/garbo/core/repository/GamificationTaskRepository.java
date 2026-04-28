package com.garbo.core.repository;

import com.garbo.core.entity.GamificationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GamificationTaskRepository extends JpaRepository<GamificationTask, Long> {

    Optional<GamificationTask> findByCode(String code);

    List<GamificationTask> findByStatusOrderByUpdatedAtDesc(String status);

    @Query("SELECT t FROM GamificationTask t WHERE t.status = 'PUBLISHED' " +
            "AND (t.roleScope = 'ALL' OR UPPER(t.roleScope) = UPPER(?1)) " +
            "AND (t.startAt IS NULL OR t.startAt <= ?2) " +
            "AND (t.endAt IS NULL OR t.endAt >= ?2) " +
            "ORDER BY t.updatedAt DESC")
    List<GamificationTask> findActivePublishedTasksForRole(String role, LocalDateTime now);
}
