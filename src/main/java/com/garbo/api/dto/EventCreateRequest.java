package com.garbo.api.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventCreateRequest {
    private String title;
    private String description;
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private String category;
    private String imageUrl;
    private Integer maxParticipants;
    /** Required when an admin creates an event (no citizen profile). */
    private String council;
}
