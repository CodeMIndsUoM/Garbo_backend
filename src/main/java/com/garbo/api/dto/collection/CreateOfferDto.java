package com.garbo.api.dto.collection;

import com.garbo.core.enums.PriceUnit;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateOfferDto(
        @Positive(message = "Price must be greater than zero")
        Double pricePerUnit,
        PriceUnit priceUnit,
        @Size(max = 255, message = "Exchange item must be at most 255 characters")
        String exchangeItem,
        @NotNull(message = "Proposed pickup time is required")
        @Future(message = "Proposed pickup time must be in the future")
        Instant proposedPickupAt,
        @Size(max = 500, message = "Message must be at most 500 characters")
        String messageToCitizen
) {}
