package com.colectivo.admin.dto.catalog;

import com.colectivo.admin.model.GeoState;
import lombok.Builder;

@Builder
public record StateResponse(
        String id,
        String countryId,
        String name,
        String code,
        boolean active,
        String createdAt,
        String updatedAt
) {
    public static StateResponse from(GeoState state) {
        return StateResponse.builder()
                .id(state.getId())
                .countryId(state.getCountryId())
                .name(state.getName())
                .code(state.getCode())
                .active(state.isActive())
                .createdAt(state.getCreatedAt() != null ? state.getCreatedAt().toString() : null)
                .updatedAt(state.getUpdatedAt() != null ? state.getUpdatedAt().toString() : null)
                .build();
    }
}
