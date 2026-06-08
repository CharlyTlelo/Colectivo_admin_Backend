package com.colectivo.admin.dto.catalog;

import com.colectivo.admin.model.Country;
import lombok.Builder;

@Builder
public record CountryResponse(
        String id,
        String name,
        String code,
        boolean active,
        String createdAt,
        String updatedAt
) {
    public static CountryResponse from(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .name(country.getName())
                .code(country.getCode())
                .active(country.isActive())
                .createdAt(country.getCreatedAt() != null ? country.getCreatedAt().toString() : null)
                .updatedAt(country.getUpdatedAt() != null ? country.getUpdatedAt().toString() : null)
                .build();
    }
}
