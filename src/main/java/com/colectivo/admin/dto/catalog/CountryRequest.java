package com.colectivo.admin.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CountryRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String code;

    private Boolean active;
}
