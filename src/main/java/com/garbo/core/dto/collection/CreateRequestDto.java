package com.garbo.core.dto.collection;

import com.garbo.core.enums.PreferredSlot;
import com.garbo.core.enums.WasteType;

import java.time.LocalDate;

public record CreateRequestDto(
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
        String photoUrl
) {}
