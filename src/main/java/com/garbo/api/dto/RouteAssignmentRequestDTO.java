package com.garbo.api.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Extends the base route session request with team assignment semantics.
 *
 * By inheriting from RouteSessionCreateRequestDTO all shared fields
 * (sessionId, userId, vehicleCount, vehicleCapacities, depotLat, depotLng,
 * selectedBinIds, vehicleId, driverId, collectorIds) and helpers
 * (hasValidTeam, hasValidDepot, getValidatedCapacities) are available
 * automatically, which allows the instanceof checks in RouteSessionService
 * to compile and behave correctly.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RouteAssignmentRequestDTO extends RouteSessionCreateRequestDTO {

    // No extra fields — everything is already declared in the parent class.
    // Add subclass-specific fields here only when new requirements arise.

    // ── toSessionRequest() removed ───────────────────────────────────────────
    // No longer needed: RouteAssignmentRequestDTO IS-A
    // RouteSessionCreateRequestDTO and can be passed directly wherever the
    // parent type is expected.
    // ─────────────────────────────────────────────────────────────────────────
}