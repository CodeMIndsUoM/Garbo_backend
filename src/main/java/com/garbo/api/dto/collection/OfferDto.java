package com.garbo.api.dto.collection;

import com.garbo.core.entity.CollectionOffer;
import com.garbo.core.enums.CancellationReason;
import com.garbo.core.enums.OfferStatus;
import com.garbo.core.enums.PriceUnit;

import java.time.Instant;

public record OfferDto(
        Long id,
        Long requestId,
        Long collectorId,
        String collectorName,
        String collectorCompany,
        Double pricePerUnit,
        PriceUnit priceUnit,
        String exchangeItem,
        Instant proposedPickupAt,
        String messageToCitizen,
        OfferStatus status,
        CancellationReason cancellationReason,
        String cancellationNote,
        Instant startedAt,
        Instant completedAt,
        String completionPhotoUrl,
        Double completionWeightKg,
        Double completionLat,
        Double completionLng,
        String completionNotes,
        Integer citizenRating,
        String citizenFeedback,
        Instant createdAt,
        Instant updatedAt
) {
    public static OfferDto from(CollectionOffer o) {
        return new OfferDto(
                o.getId(),
                o.getRequest() != null ? o.getRequest().getId() : null,
                o.getCollector() != null ? o.getCollector().getEmpId() : null,
                o.getCollector() != null ? o.getCollector().getEmpName() : null,
                o.getCollector() != null ? o.getCollector().getCompany() : null,
                o.getPricePerUnit(),
                o.getPriceUnit(),
                o.getExchangeItem(),
                o.getProposedPickupAt(),
                o.getMessageToCitizen(),
                o.getStatus(),
                o.getCancellationReason(),
                o.getCancellationNote(),
                o.getStartedAt(),
                o.getCompletedAt(),
                o.getCompletionPhotoUrl(),
                o.getCompletionWeightKg(),
                o.getCompletionLat(),
                o.getCompletionLng(),
                o.getCompletionNotes(),
                o.getCitizenRating(),
                o.getCitizenFeedback(),
                o.getCreatedAt(),
                o.getUpdatedAt()
        );
    }
}
