package com.garbo.core.dto.collection;

import com.garbo.core.entity.CollectionRequest;
import com.garbo.core.enums.PreferredSlot;
import com.garbo.core.enums.RequestStatus;
import com.garbo.core.enums.WasteType;

import java.time.Instant;
import java.time.LocalDate;

public record RequestSummaryDto(
        Long id,
        Long citizenId,
        String citizenName,
        WasteType wasteType,
        String quantityLabel,
        Double quantityKgEstimate,
        String addressLine,
        Double latitude,
        Double longitude,
        LocalDate preferredDate,
        PreferredSlot preferredSlot,
        String contactPhone,
        String notes,
        String photoUrl,
        RequestStatus status,
        Long acceptedOfferId,
        int offersCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static RequestSummaryDto from(CollectionRequest r, int offersCount) {
        return new RequestSummaryDto(
                r.getId(),
                r.getCitizen() != null ? r.getCitizen().getEmpId() : null,
                r.getCitizen() != null ? r.getCitizen().getEmpName() : null,
                r.getWasteType(),
                r.getQuantityLabel(),
                r.getQuantityKgEstimate(),
                r.getAddressLine(),
                r.getLatitude(),
                r.getLongitude(),
                r.getPreferredDate(),
                r.getPreferredSlot(),
                r.getContactPhone(),
                r.getNotes(),
                r.getPhotoUrl(),
                r.getStatus(),
                r.getAcceptedOfferId(),
                offersCount,
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
