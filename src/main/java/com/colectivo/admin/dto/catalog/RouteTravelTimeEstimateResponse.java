package com.colectivo.admin.dto.catalog;

import lombok.Builder;

@Builder
public record RouteTravelTimeEstimateResponse(
        String originLocalityId,
        String destinationLocalityId,
        boolean available,
        Integer estimatedTravelMinutes,
        Double distanceKm,
        String message
) {
}
