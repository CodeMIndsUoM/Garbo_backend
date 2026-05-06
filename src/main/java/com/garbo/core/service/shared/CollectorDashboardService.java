package com.garbo.core.service.shared;

import com.garbo.core.dto.collection.CollectorDashboardDto;
import com.garbo.core.entity.CollectionOffer;
import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.enums.OfferStatus;
import com.garbo.core.enums.RequestStatus;
import com.garbo.core.repository.CollectionOfferRepository;
import com.garbo.core.repository.CollectionRequestRepository;
import com.garbo.core.repository.ThirdPartyCollectorRepository;
import com.garbo.core.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class CollectorDashboardService {

    private final CollectionRequestRepository requestRepository;
    private final CollectionOfferRepository offerRepository;
    private final UserRepository userRepository;
    private final ThirdPartyCollectorRepository collectorRepository;

    public CollectorDashboardService(
            CollectionRequestRepository requestRepository,
            CollectionOfferRepository offerRepository,
            UserRepository userRepository,
            ThirdPartyCollectorRepository collectorRepository) {
        this.requestRepository = requestRepository;
        this.offerRepository = offerRepository;
        this.userRepository = userRepository;
        this.collectorRepository = collectorRepository;
    }

    @Transactional(readOnly = true)
    public CollectorDashboardDto getCollectorDashboard(Long collectorId) {
        List<String> assignedCouncils = collectorRepository.findById(collectorId)
                .map(this::normalizedAssignedCouncils)
                .orElse(List.of());
        int availableRequests = assignedCouncils.isEmpty()
                ? 0
                : (int) requestRepository.countByStatusAndCouncilIn(RequestStatus.OPEN, assignedCouncils);
        int activeJobs = (int) offerRepository.countByCollector_EmpIdAndStatusIn(
                collectorId,
                List.of(OfferStatus.ACCEPTED, OfferStatus.IN_PROGRESS));
        int completedJobs = (int) offerRepository.countByCollector_EmpIdAndStatusIn(
                collectorId,
                List.of(OfferStatus.COMPLETED));

        List<CollectionOffer> completedOffers = offerRepository
                .findByCollector_EmpIdAndStatusOrderByCreatedAtDesc(collectorId, OfferStatus.COMPLETED);

        DashboardAggregates aggregates = aggregateDashboardMetrics(completedOffers);

        List<CollectionOffer> allOffers = offerRepository.findByCollector_EmpIdOrderByCreatedAtDesc(collectorId);
        int cancelledOffers = 0;
        for (CollectionOffer offer : allOffers) {
            if (offer.getStatus() == OfferStatus.CANCELLED || offer.getStatus() == OfferStatus.WITHDRAWN) {
                cancelledOffers++;
            }
        }

        int onTimeCount = 0;
        for (CollectionOffer offer : completedOffers) {
            if (offer.getProposedPickupAt() != null
                    && offer.getCompletedAt() != null
                    && offer.getCompletedAt().isBefore(offer.getProposedPickupAt().plusSeconds(24 * 60 * 60))) {
                onTimeCount++;
            }
        }

        double responseRate = allOffers.isEmpty()
                ? 100.0
                : Math.max(0.0, 100.0 - ((double) cancelledOffers / allOffers.size()) * 100.0);
        double onTimeRate = completedOffers.isEmpty()
                ? 100.0
                : ((double) onTimeCount / completedOffers.size()) * 100.0;

        Instant memberSince = userRepository.findById(collectorId)
                .map(u -> u.getCreatedAt())
                .filter(Objects::nonNull)
                .map(ldt -> ldt.atZone(ZoneId.systemDefault()).toInstant())
                .orElse(null);

        return new CollectorDashboardDto(
                availableRequests,
                activeJobs,
                completedJobs,
                aggregates.todaysRating(),
                aggregates.todaysWorkingMinutes(),
                aggregates.todaysWasteCollectedKg(),
                responseRate,
                onTimeRate,
                aggregates.overallRating(),
                aggregates.overallRatedCount(),
                memberSince);
    }

    private DashboardAggregates aggregateDashboardMetrics(List<CollectionOffer> completedOffers) {
        double todaysRatingSum = 0;
        int todaysRatedCount = 0;
        int todaysWorkingMinutes = 0;
        double todaysWasteCollectedKg = 0;

        double overallRatingSum = 0;
        int overallRatedCount = 0;

        Instant startOfToday = LocalDate.now(ZoneId.systemDefault())
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        for (CollectionOffer offer : completedOffers) {
            boolean isToday = offer.getCompletedAt() != null && !offer.getCompletedAt().isBefore(startOfToday);

            if (offer.getCitizenRating() != null && offer.getCitizenRating() > 0) {
                overallRatingSum += offer.getCitizenRating();
                overallRatedCount++;
                if (isToday) {
                    todaysRatingSum += offer.getCitizenRating();
                    todaysRatedCount++;
                }
            }

            if (isToday) {
                if (offer.getCompletionWeightKg() != null) {
                    todaysWasteCollectedKg += offer.getCompletionWeightKg();
                }
                if (offer.getStartedAt() != null && offer.getCompletedAt() != null) {
                    todaysWorkingMinutes += java.time.Duration.between(
                            offer.getStartedAt(),
                            offer.getCompletedAt()).toMinutes();
                }
            }
        }

        double todaysRating = todaysRatedCount > 0 ? (todaysRatingSum / todaysRatedCount) : 0.0;
        double overallRating = overallRatedCount > 0 ? (overallRatingSum / overallRatedCount) : 0.0;

        return new DashboardAggregates(
                todaysRating,
                todaysWorkingMinutes,
                todaysWasteCollectedKg,
                overallRating,
                overallRatedCount);
    }

    private record DashboardAggregates(
            double todaysRating,
            int todaysWorkingMinutes,
            double todaysWasteCollectedKg,
            double overallRating,
            int overallRatedCount) {
    }

    private List<String> normalizedAssignedCouncils(ThirdPartyCollector collector) {
        if (collector.getAssignedCouncils() == null || collector.getAssignedCouncils().trim().isEmpty()) {
            return List.of();
        }
        return List.of(collector.getAssignedCouncils().split(","))
                .stream()
                .map(council -> council == null ? "" : council.trim().toLowerCase(Locale.ROOT))
                .filter(council -> !council.isBlank())
                .distinct()
                .toList();
    }
}
