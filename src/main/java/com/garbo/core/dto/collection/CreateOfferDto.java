package com.garbo.core.dto.collection;

import com.garbo.core.enums.PriceUnit;

import java.time.Instant;

public record CreateOfferDto(
        Double pricePerUnit,
        PriceUnit priceUnit,
        Instant proposedPickupAt,
        String messageToCitizen
) {}
