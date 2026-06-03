package com.garbo.api.dto.collection;

import com.garbo.core.enums.CancellationReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CancelOfferDto(
        @NotNull(message = "Cancellation reason is required")
        CancellationReason reason,
        @Size(max = 500, message = "Cancellation note must be at most 500 characters")
        String note
) {}
