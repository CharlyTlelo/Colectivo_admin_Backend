package com.colectivo.admin.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StateRequest {
    @NotBlank
    private String countryId;

    @NotBlank
    private String name;

    @NotBlank
    private String code;

    private Boolean active;
}
