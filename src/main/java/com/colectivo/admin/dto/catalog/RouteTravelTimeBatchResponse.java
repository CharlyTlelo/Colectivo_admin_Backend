package com.colectivo.admin.dto.catalog;

import java.util.List;
import lombok.Builder;

@Builder
public record RouteTravelTimeBatchResponse(
        List<RouteTravelTimeResponse> results,
        int requestedCombinations,
        int skippedSamePointCombinations
) {
}
