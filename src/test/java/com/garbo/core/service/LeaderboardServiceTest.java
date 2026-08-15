package com.garbo.core.service;

import com.garbo.api.dto.websocket.LeaderboardUpdatePayload;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
import com.garbo.core.repository.BinCollectorRepository;
import com.garbo.core.repository.FieldMentorRepository;
import com.garbo.core.repository.ScoreTransactionRepository;
import com.garbo.core.repository.UserTaskProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class LeaderboardServiceTest {

    private BinCollectorRepository binCollectorRepository;
    private FieldMentorRepository fieldMentorRepository;
    private ScoreTransactionRepository scoreTransactionRepository;
    private UserTaskProgressRepository userTaskProgressRepository;
    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        binCollectorRepository = Mockito.mock(BinCollectorRepository.class);
        fieldMentorRepository = Mockito.mock(FieldMentorRepository.class);
        scoreTransactionRepository = Mockito.mock(ScoreTransactionRepository.class);
        userTaskProgressRepository = Mockito.mock(UserTaskProgressRepository.class);

        leaderboardService = new LeaderboardService();

        org.springframework.test.util.ReflectionTestUtils.setField(
                leaderboardService,
                "binCollectorRepository",
                binCollectorRepository
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                leaderboardService,
                "fieldMentorRepository",
                fieldMentorRepository
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                leaderboardService,
                "scoreTransactionRepository",
                scoreTransactionRepository
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                leaderboardService,
                "userTaskProgressRepository",
                userTaskProgressRepository
        );
    }

    @Test
    void getTopLeaderboard_sortsByRewardPointsDescending() {
        BinCollector collector1 = new BinCollector();
        collector1.setEmpId(1L);
        collector1.setEmpName("Collector Bob");
        collector1.setRewardPoints(150.0);

        BinCollector collector2 = new BinCollector();
        collector2.setEmpId(2L);
        collector2.setEmpName("Collector Alice");
        collector2.setRewardPoints(300.0);

        when(binCollectorRepository.findAll()).thenReturn(List.of(collector1, collector2));
        when(fieldMentorRepository.findAll()).thenReturn(Collections.emptyList());
        when(scoreTransactionRepository.findTaskScoreAggregates()).thenReturn(Collections.emptyList());
        when(userTaskProgressRepository.findCompletedPointsByUser()).thenReturn(Collections.emptyList());

        List<LeaderboardUpdatePayload.LeaderboardEntryDto> res = leaderboardService.getTopLeaderboard(10, "COLLECTOR");

        assertEquals(2, res.size());
        assertEquals("Collector Alice", res.get(0).getName()); // Top 1 with 300 points
        assertEquals("Collector Bob", res.get(1).getName());   // Top 2 with 150 points
        assertEquals(1, res.get(0).getRank());
        assertEquals(2, res.get(1).getRank());
    }
}
