package com.garbo.core.repository;

import com.garbo.core.entity.UserNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<UserNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    Optional<UserNotification> findByIdAndUserId(UUID id, Long userId);

    Optional<UserNotification> findByUserIdAndSourceEventId(Long userId, String sourceEventId);

    @Modifying
    @Query("UPDATE UserNotification n SET n.read = true, n.readAt = :readAt WHERE n.userId = :userId AND n.read = false")
    int markAllRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    List<UserNotification> findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime createdAt,
            Pageable pageable
    );
}
