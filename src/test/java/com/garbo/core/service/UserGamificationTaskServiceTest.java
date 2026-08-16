package com.garbo.core.service;

import com.garbo.api.dto.gamification.UserGamificationTaskProgressResponse;
import com.garbo.core.entity.GamificationTask;
import com.garbo.core.entity.UserTaskProgress;
import com.garbo.core.repository.UserTaskProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class UserGamificationTaskServiceTest {

    private GamificationTaskService gamificationTaskService;
    private UserTaskProgressRepository userTaskProgressRepository;
    private UserGamificationTaskService userGamificationTaskService;

    @BeforeEach
    void setUp() {
        gamificationTaskService = Mockito.mock(GamificationTaskService.class);
        userTaskProgressRepository = Mockito.mock(UserTaskProgressRepository.class);
        userGamificationTaskService = new UserGamificationTaskService();

        org.springframework.test.util.ReflectionTestUtils.setField(
                userGamificationTaskService,
                "gamificationTaskService",
                gamificationTaskService
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                userGamificationTaskService,
                "userTaskProgressRepository",
                userTaskProgressRepository
        );
    }

    @Test
    void getUserTaskProgress_returnsProgressDTOs() {
        GamificationTask task1 = new GamificationTask();
        task1.setId(10L);
        task1.setCode("T10");
        task1.setTitle("Collect Bins");
        task1.setDescription("Collect 5 bins");
        task1.setBasePoints(50.0);
        task1.setTargetProgress(5.0);
        task1.setTaskType("BIN_REPORT");

        UserTaskProgress progress1 = new UserTaskProgress();
        progress1.setId(100L);
        progress1.setTask(task1);
        progress1.setCurrentProgress(3.0);
        progress1.setTargetProgress(5.0);
        progress1.setCompleted(false);
        progress1.setPointsEarned(0.0);

        when(gamificationTaskService.getActiveTasksForRole("BIN_COLLECTOR")).thenReturn(List.of(task1));
        when(userTaskProgressRepository.findByUserId(123L)).thenReturn(List.of(progress1));

        List<UserGamificationTaskProgressResponse> res = userGamificationTaskService.getUserTaskProgress(123L, "BIN_COLLECTOR");

        assertEquals(1, res.size());
        assertEquals("T10", res.get(0).getTaskCode());
        assertEquals(3.0, res.get(0).getCurrentProgress());
        assertEquals(5.0, res.get(0).getTargetProgress());
        assertFalse(res.get(0).isCompleted());
    }
}
