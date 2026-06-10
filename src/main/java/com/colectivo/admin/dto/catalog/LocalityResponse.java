package com.colectivo.admin.dto.catalog;

import com.colectivo.admin.model.Locality;
import com.colectivo.admin.model.LocalityType;
import lombok.Builder;

@Builder
public record LocalityResponse(
        String id,
        String municipalityId,
        String name,
        LocalityType type,
        boolean active,
        Integer estimatedTravelMinutes,
        Integer returnTravelMinutes,
        String travelTimeCalculatedAt,
        String createdAt,
        String updatedAt
) {
    public static LocalityResponse from(Locality locality) {
        return LocalityResponse.builder()
                .id(locality.getId())
                .municipalityId(locality.getMunicipalityId())
                .name(locality.getName())
                .type(locality.getType())
                .active(locality.isActive())
                .estimatedTravelMinutes(locality.getEstimatedTravelMinutes())
                .returnTravelMinutes(locality.getReturnTravelMinutes())
                .travelTimeCalculatedAt(locality.getTravelTimeCalculatedAt() != null ? locality.getTravelTimeCalculatedAt().toString() : null)
                .createdAt(locality.getCreatedAt() != null ? locality.getCreatedAt().toString() : null)
                .updatedAt(locality.getUpdatedAt() != null ? locality.getUpdatedAt().toString() : null)
                .build();
    }
}
