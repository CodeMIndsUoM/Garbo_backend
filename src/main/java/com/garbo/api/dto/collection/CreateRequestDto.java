package com.garbo.api.dto.collection;

import com.garbo.core.enums.PreferredSlot;
import com.garbo.core.enums.WasteType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateRequestDto(
        WasteType wasteType,
        List<WasteType> wasteTypes,
        @NotBlank(message = "Quantity label is required")
        @Size(max = 50, message = "Quantity label must be at most 50 characters")
        String quantityLabel,
        @Positive(message = "Quantity estimate must be greater than zero")
        Double quantityKgEstimate,
        @NotBlank(message = "Address is required")
        @Size(max = 500, message = "Address must be at most 500 characters")
        String addressLine,
        @NotNull(message = "Latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
        @DecimalMax(value = "90.0", message = "Latitude must be at most 90")
        Double latitude,
        @NotNull(message = "Longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
        Double longitude,
        @NotNull(message = "Preferred date is required")
        @FutureOrPresent(message = "Preferred date must be today or later")
        LocalDate preferredDate,
        @NotNull(message = "Preferred slot is required")
        PreferredSlot preferredSlot,
        @NotBlank(message = "Contact phone is required")
        @Size(max = 20, message = "Contact phone must be at most 20 characters")
        String contactPhone,
        @Size(max = 2000, message = "Notes must be at most 2000 characters")
        String notes,
        @Size(max = 500, message = "Photo URL must be at most 500 characters")
        String photoUrl
) {}
