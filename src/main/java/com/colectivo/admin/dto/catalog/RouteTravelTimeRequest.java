package com.colectivo.admin.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RouteTravelTimeRequest {
    @NotBlank
    private String originLocalityId;

    @NotBlank
    private String destinationLocalityId;
}
