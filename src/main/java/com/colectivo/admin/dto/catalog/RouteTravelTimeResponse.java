package com.colectivo.admin.dto.catalog;

import lombok.Builder;

import java.time.Instant;

@Builder
public record RouteTravelTimeResponse(
        String originLocalityId,
        String destinationLocalityId,
        String originLabel,
        String destinationLabel,
        int estimatedTravelMinutes,
        double distanceKm,
        Instant calculatedAt
) {
}
