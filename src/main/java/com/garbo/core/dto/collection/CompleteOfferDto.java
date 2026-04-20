package com.garbo.core.dto.collection;

public record CompleteOfferDto(
        String photoUrl,
        Double weightKg,
        Double latitude,
        Double longitude,
        String notes
) {}
