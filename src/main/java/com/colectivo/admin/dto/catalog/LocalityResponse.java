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
        String label,
        Integer estimatedTravelMinutes,
        Integer returnTravelMinutes,
        String travelTimeCalculatedAt,
        Double latitude,
        Double longitude,
        Double manualLatitude,
        Double manualLongitude,
        boolean hasManualLocation,
        String mapsUrl,
        String manualLocationSetAt,
        String createdAt,
        String updatedAt
) {
    public static LocalityResponse from(Locality locality) {
        boolean manual = locality.getManualLatitude() != null && locality.getManualLongitude() != null;
        return LocalityResponse.builder()
                .id(locality.getId())
                .municipalityId(locality.getMunicipalityId())
                .name(locality.getName())
                .type(locality.getType())
                .active(locality.isActive())
                .label(locality.getLabel())
                .estimatedTravelMinutes(locality.getEstimatedTravelMinutes())
                .returnTravelMinutes(locality.getReturnTravelMinutes())
                .travelTimeCalculatedAt(locality.getTravelTimeCalculatedAt() != null ? locality.getTravelTimeCalculatedAt().toString() : null)
                .latitude(locality.getLatitude())
                .longitude(locality.getLongitude())
                .manualLatitude(locality.getManualLatitude())
                .manualLongitude(locality.getManualLongitude())
                .hasManualLocation(manual)
                .mapsUrl(locality.getMapsUrl())
                .manualLocationSetAt(locality.getManualLocationSetAt() != null ? locality.getManualLocationSetAt().toString() : null)
                .createdAt(locality.getCreatedAt() != null ? locality.getCreatedAt().toString() : null)
                .updatedAt(locality.getUpdatedAt() != null ? locality.getUpdatedAt().toString() : null)
                .build();
    }
}
