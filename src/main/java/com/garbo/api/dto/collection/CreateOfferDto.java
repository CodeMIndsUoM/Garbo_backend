package com.garbo.api.dto.collection;

import com.garbo.core.enums.PriceUnit;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateOfferDto(
        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        Double pricePerUnit,
        @NotNull(message = "Price unit is required")
        PriceUnit priceUnit,
        @NotNull(message = "Proposed pickup time is required")
        @Future(message = "Proposed pickup time must be in the future")
        Instant proposedPickupAt,
        @Size(max = 500, message = "Message must be at most 500 characters")
        String messageToCitizen
) {}
