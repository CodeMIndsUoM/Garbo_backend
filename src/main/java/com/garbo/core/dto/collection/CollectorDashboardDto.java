package com.garbo.core.dto.collection;

public record CollectorDashboardDto(
    int availableRequests,
    int activeJobs,
    int completedJobs,
    double todaysRating,
    int todaysWorkingMinutes,
    double todaysWasteCollectedKg,
    double responseRate,
    double onTimeRate,
    double overallRating
) {
}
