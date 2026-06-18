package com.garbo.core.service;

import com.garbo.core.entity.Bin;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.User;
import com.garbo.core.entity.UserTaskProgress;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.websocket.TaskProgressBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BinCollectionRealtimeService {

    @Autowired
    private BinCollectorRepository binCollectorRepository;

    @Autowired
    private BinRepository binRepository;

        @Autowired
        private UserRepository userRepository;

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private UserTaskProgressService userTaskProgressService;

    @Autowired
    private TaskProgressBroadcaster taskProgressBroadcaster;

    @Transactional
    public BinCollectionResult processBinCollected(
            Long userId,
            Long binId,
            String priority,
            Double basePoints,
            String sessionId
    ) {
                BinCollector collector = resolveCollectorOrCreateIfAllowed(userId);
                if (collector == null) {
                        throw new IllegalArgumentException("Collector not found for userId=" + userId);
                }

                Bin bin = binRepository.findByIdCastToBigInt(binId);
        String resolvedPriority = priority;
        if (resolvedPriority == null || resolvedPriority.isBlank()) {
                        resolvedPriority = bin != null && bin.getPriority() != null && !((String) bin.getPriority()).isBlank()
                                        ? (String) bin.getPriority()
                                        : "MEDIUM";
        }

        double resolvedBasePoints = basePoints != null && basePoints > 0 ? basePoints : 10.0;

        collector.setCompletedCollections(collector.getCompletedCollections() + 1);
        binCollectorRepository.save(collector);

        List<UserTaskProgress> updatedTasks = userTaskProgressService.incrementActiveBinTasksForCollection(
                userId,
                "COLLECTOR",
                binId,
                resolvedPriority,
                sessionId
        );

        taskProgressBroadcaster.broadcastTaskProgressUpdate(
                userId,
                binId,
                collector.getCompletedCollections(),
                updatedTasks
        );

        return new BinCollectionResult(
                userId,
                binId,
                collector.getCompletedCollections(),
                resolvedPriority,
                resolvedBasePoints,
                updatedTasks.size()
        );
    }

    public record BinCollectionResult(
            Long userId,
            Long binId,
            int totalBinsCollected,
            String priority,
            double basePoints,
            int affectedTasks
    ) {
    }

        private BinCollector resolveCollectorOrCreateIfAllowed(Long userId) {
                if (userId == null) {
                        return null;
                }

                var existing = binCollectorRepository.findById(userId);
                if (existing.isPresent()) {
                        return existing.get();
                }

                User user = userRepository.findById(userId).orElse(null);
                if (user == null || !isCollectorRole(user.getRole())) {
                        return null;
                }

                BinCollector created = new BinCollector();
                created.setEmpId(user.getEmpId());
                created.setEmpName(user.getEmpName());
                created.setEmail(user.getEmail());
                created.setPassword(user.getPassword());
                created.setRole(user.getRole());
                created.setPhone(user.getPhone());
                created.setCreatedAt(user.getCreatedAt());
                created.setLastLoginAt(user.getLastLoginAt());
                created.setAssignedCouncil(null);
                created.setTeam(null);
                created.setWorkShift(null);
                created.setOnDuty(false);
                created.setCompletedCollections(0);
                created.setMissedCollections(0);
                created.setRewardPoints(0.0);

                log.info("Auto-provisioning missing collector profile during bin collection for userId={}", userId);
                return binCollectorRepository.save(created);
        }

        private boolean isCollectorRole(String role) {
                if (role == null || role.isBlank()) {
                        return true;
                }
                String normalized = role.trim().toUpperCase();
                return "COLLECTOR".equals(normalized)
                                || "BIN_COLLECTOR".equals(normalized)
                                || "COLLECTION_TEAM".equals(normalized);
        }
}
