package com.garbo.infrastructure.websocket;

import com.garbo.api.dto.websocket.LeaderboardUpdatePayload;
import com.garbo.api.dto.websocket.WebSocketMessage;
import com.garbo.core.service.LeaderboardService;
import com.garbo.core.service.event.ScoreAwardedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Broadcasts leaderboard updates to all connected clients.
 * Listens for score changes and pushes real-time updates.
 */
@Slf4j
@Component
public class LeaderboardBroadcaster {
    
    @Autowired
    private WebSocketSessionManager sessionManager;
    
    @Autowired
    private LeaderboardService leaderboardService;

    // Previous top-N snapshot used to compute rank delta on each realtime push.
    private final Map<String, PreviousEntry> previousTopEntriesByKey = new HashMap<>();
    
    /**
     * Broadcast updated leaderboard to all connected users.
     */
    public void broadcastLeaderboardUpdate() {
        broadcastLeaderboardUpdate(null);
    }

    public synchronized void broadcastLeaderboardUpdate(ScoreAwardedEvent triggerEvent) {
        try {
            // Get top 10 leaderboard entries
            List<LeaderboardUpdatePayload.LeaderboardEntryDto> topEntries = 
                    leaderboardService.getTopLeaderboard(10);
            
            if (topEntries.isEmpty()) {
                log.debug("No leaderboard entries to broadcast");
                return;
            }

            Map<String, PreviousEntry> previousSnapshot = new HashMap<>(previousTopEntriesByKey);
            for (LeaderboardUpdatePayload.LeaderboardEntryDto entry : topEntries) {
                String key = entryKey(entry.getUserId(), entry.getRole());
                PreviousEntry previous = previousSnapshot.get(key);
                if (previous != null) {
                    // Positive means moved up in ranking (e.g. 5 -> 3 gives +2).
                    entry.setRankChangeFromPrevious(previous.rank() - entry.getRank());
                } else {
                    entry.setRankChangeFromPrevious(null);
                }
            }

            LeaderboardUpdatePayload.ChangedUserContext changedUser = buildChangedUserContext(triggerEvent, topEntries, previousSnapshot);
            
            // Create broadcast message
            LeaderboardUpdatePayload payload = new LeaderboardUpdatePayload(
                    topEntries,
                    System.currentTimeMillis(),
                    changedUser
            );
            
            WebSocketMessage<LeaderboardUpdatePayload> message = new WebSocketMessage<>(
                    "LEADERBOARD_UPDATE",
                    null,
                    payload
            );
            
            // Broadcast to all connected users
            sessionManager.broadcastToAll(message);

            previousTopEntriesByKey.clear();
            for (LeaderboardUpdatePayload.LeaderboardEntryDto entry : topEntries) {
                previousTopEntriesByKey.put(
                        entryKey(entry.getUserId(), entry.getRole()),
                        new PreviousEntry(entry.getRank(), entry.getRewardPoints())
                );
            }
            
            log.info("Broadcast leaderboard update to {} connected users with {} entries",
                    sessionManager.getConnectedUserCount(), topEntries.size());
            
        } catch (Exception e) {
            log.error("Error broadcasting leaderboard update: {}", e.getMessage());
        }
    }

    @EventListener
    public void onScoreAwarded(ScoreAwardedEvent event) {
        try {
            log.info(
                    "Score awarded event received: userId={}, role={}, taskId={}, sourceEventId={}",
                    event.getUserId(),
                    event.getRole(),
                    event.getTaskId(),
                    event.getSourceEventId()
            );
            broadcastLeaderboardUpdate(event);
        } catch (Exception e) {
            log.error("Error broadcasting leaderboard after score-awarded event: {}", e.getMessage(), e);
        }
    }

    private LeaderboardUpdatePayload.ChangedUserContext buildChangedUserContext(
            ScoreAwardedEvent triggerEvent,
            List<LeaderboardUpdatePayload.LeaderboardEntryDto> topEntries,
            Map<String, PreviousEntry> previousSnapshot
    ) {
        if (triggerEvent == null || triggerEvent.getUserId() == null || triggerEvent.getRole() == null) {
            return null;
        }

        String key = entryKey(triggerEvent.getUserId(), triggerEvent.getRole());
        PreviousEntry previous = previousSnapshot.get(key);

        LeaderboardUpdatePayload.LeaderboardEntryDto current = null;
        for (LeaderboardUpdatePayload.LeaderboardEntryDto entry : topEntries) {
            if (isSameUser(entry, triggerEvent.getUserId(), triggerEvent.getRole())) {
                current = entry;
                break;
            }
        }

        Integer previousRank = previous != null ? previous.rank() : null;
        Integer currentRank = current != null ? current.getRank() : null;
        Integer rankDelta = previousRank != null && currentRank != null ? previousRank - currentRank : null;

        Double previousScore = previous != null ? previous.rewardPoints() : null;
        Double currentScore = current != null ? current.getRewardPoints() : null;
        Double scoreDelta = previousScore != null && currentScore != null ? currentScore - previousScore : null;

        boolean enteredTop = previousRank == null && currentRank != null;
        boolean exitedTop = previousRank != null && currentRank == null;

        return new LeaderboardUpdatePayload.ChangedUserContext(
                triggerEvent.getUserId(),
                triggerEvent.getRole(),
                triggerEvent.getTaskId(),
                triggerEvent.getSourceEventId(),
                previousRank,
                currentRank,
                rankDelta,
                previousScore,
                currentScore,
                scoreDelta,
                enteredTop,
                exitedTop
        );
    }

    private boolean isSameUser(LeaderboardUpdatePayload.LeaderboardEntryDto entry, Long userId, String role) {
        return userId.equals(entry.getUserId()) && role.equalsIgnoreCase(entry.getRole());
    }

    private String entryKey(Long userId, String role) {
        return role.toUpperCase() + ":" + (userId != null ? userId : -1L);
    }

    private record PreviousEntry(int rank, double rewardPoints) {
    }
    
    /**
     * Called when points are awarded to a collector.
     * Refreshes leaderboard and broadcasts update.
     */
    public void onPointsAwarded(Long collectorId) {
        try {
            log.info("Points awarded to collector {}. Refreshing leaderboard.", collectorId);
            leaderboardService.refreshLeaderboard();
            broadcastLeaderboardUpdate();
        } catch (Exception e) {
            log.error("Error handling points award for collector {}: {}", collectorId, e.getMessage());
        }
    }
}
