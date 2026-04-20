package com.garbo.core.dto.collection;

import com.garbo.core.enums.CancellationReason;

public record CancelOfferDto(
        CancellationReason reason,
        String note
) {}
