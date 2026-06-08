package com.garbo.api.dto.collection;

import java.time.Instant;

public record CollectorDashboardDto(
    int availableRequests,
    int activeJobs,
    int completedJobs,
    double todaysRating,
    int todaysWorkingMinutes,
    double todaysWasteCollectedKg,
    double responseRate,
    double onTimeRate,
    double overallRating,
    int totalReviews,
    Instant memberSince
) {
}
