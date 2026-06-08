package com.colectivo.admin.dto.catalog;

import com.colectivo.admin.model.Municipality;
import com.colectivo.admin.model.MunicipalityType;
import lombok.Builder;

@Builder
public record MunicipalityResponse(
        String id,
        String stateId,
        String name,
        MunicipalityType type,
        boolean active,
        String createdAt,
        String updatedAt
) {
    public static MunicipalityResponse from(Municipality municipality) {
        return MunicipalityResponse.builder()
                .id(municipality.getId())
                .stateId(municipality.getStateId())
                .name(municipality.getName())
                .type(municipality.getType())
                .active(municipality.isActive())
                .createdAt(municipality.getCreatedAt() != null ? municipality.getCreatedAt().toString() : null)
                .updatedAt(municipality.getUpdatedAt() != null ? municipality.getUpdatedAt().toString() : null)
                .build();
    }
}
