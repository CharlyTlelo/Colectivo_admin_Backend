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
                .createdAt(locality.getCreatedAt() != null ? locality.getCreatedAt().toString() : null)
                .updatedAt(locality.getUpdatedAt() != null ? locality.getUpdatedAt().toString() : null)
                .build();
    }
}
