package com.colectivo.admin.dto.catalog;

import com.colectivo.admin.model.LocalityType;
import lombok.Builder;

@Builder
public record LocalityOptionResponse(
        String id,
        String name,
        String municipalityName,
        String stateName,
        LocalityType type,
        String label
) {
}
