package com.garbo.core.service;

import com.garbo.api.dto.Collect_analyze_dtos.DashboardResponseDTO;
import com.garbo.core.repository.CollectorRouteCompletionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsDashboardServiceTest {

    @Mock
    private CollectorRouteCompletionRepository repo;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void analytics_weekFilter_usesWeeklyGrouping() {
        ReflectionTestUtils.setField(analyticsService, "repo", repo);

        when(repo.getSummary(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(new Object[]{7, 5, 2}));
        when(repo.getDailyData(any(LocalDateTime.class)))
            .thenReturn(Collections.singletonList(new Object[]{"2024-W01", 7, 5, 2}));

        DashboardResponseDTO result = analyticsService.getDashboard("WEEK", null);

        assertNotNull(result);
        assertEquals(7, result.getAssigned());
        assertEquals(5, result.getCollected());
        assertEquals(2, result.getMissed());
        assertEquals("2024-W01", result.getChartData().get(0).getTime());
    }

    @Test
    void analytics_councilFilter_returnsCouncilSpecificTotals() {
        ReflectionTestUtils.setField(analyticsService, "repo", repo);

        when(repo.getSummaryByCouncil(any(LocalDateTime.class), eq("KMC")))
            .thenReturn(Collections.singletonList(new Object[]{12, 9, 3}));
        when(repo.getWeeklyDataByCouncil(any(LocalDateTime.class), eq("KMC")))
            .thenReturn(Collections.singletonList(new Object[]{"2024-W02", 12, 9, 3}));

        DashboardResponseDTO result = analyticsService.getDashboard("MONTH", "KMC");

        assertNotNull(result);
        assertEquals(12, result.getAssigned());
        assertEquals(9, result.getCollected());
        assertEquals(3, result.getMissed());
        assertEquals("2024-W02", result.getChartData().get(0).getTime());
    }

    @Test
    void analytics_noData_returnsZeroSummary() {
        ReflectionTestUtils.setField(analyticsService, "repo", repo);

        when(repo.getSummary(any(LocalDateTime.class))).thenReturn(List.of());
        when(repo.getHourlyData(any(LocalDateTime.class))).thenReturn(List.of());

        DashboardResponseDTO result = analyticsService.getDashboard("DAY", null);

        assertNotNull(result);
        assertEquals(0, result.getAssigned());
        assertEquals(0, result.getCollected());
        assertEquals(0, result.getMissed());
        assertEquals(0, result.getChartData().size());
    }
}
