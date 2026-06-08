package com.garbo.core.repository;

import com.garbo.core.entity.Leaderboard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Optional;

@Repository
public interface LeaderboardRepository extends JpaRepository<Leaderboard, Long> {
    
    /**
     * Get top N leaderboard entries for a specific date, ordered by rank.
     */
    @Query("SELECT l FROM Leaderboard l WHERE l.snapshotDate = ?1 ORDER BY l.rank ASC LIMIT ?2")
    List<Leaderboard> findTopByDate(LocalDate date, int limit);
    
    /**
     * Get top N leaderboard entries for today, ordered by rank.
     */
    @Query(value = "SELECT * FROM leaderboards WHERE snapshot_date = CURRENT_DATE ORDER BY rank ASC LIMIT ?1", nativeQuery = true)
    List<Leaderboard> findTopToday(int limit);
    
    /**
     * Get a specific collector's rank for today.
     */
    @Query(value = "SELECT * FROM leaderboards WHERE collector_id = ?1 AND snapshot_date = CURRENT_DATE", nativeQuery = true)
    Optional<Leaderboard> findCollectorTodayRank(Long collectorId);
    
    /**
     * Get all leaderboard entries for a date.
     */
    List<Leaderboard> findBySnapshotDate(LocalDate date);

    Optional<Leaderboard> findFirstByUserIdAndRole(Long userId, String role);

    List<Leaderboard> findByRole(String role);
}
