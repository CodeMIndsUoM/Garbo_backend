package com.garbo.api.dto.collection;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CompleteOfferDto(
        @NotBlank(message = "Completion photo URL is required")
        @Size(max = 500, message = "Completion photo URL must be at most 500 characters")
        String photoUrl,
        @Positive(message = "Weight must be greater than zero")
        Double weightKg,
        @NotNull(message = "Completion latitude is required")
        @DecimalMin(value = "-90.0", message = "Latitude must be at least -90")
        @DecimalMax(value = "90.0", message = "Latitude must be at most 90")
        Double latitude,
        @NotNull(message = "Completion longitude is required")
        @DecimalMin(value = "-180.0", message = "Longitude must be at least -180")
        @DecimalMax(value = "180.0", message = "Longitude must be at most 180")
        Double longitude,
        @Size(max = 2000, message = "Notes must be at most 2000 characters")
        String notes
) {}
