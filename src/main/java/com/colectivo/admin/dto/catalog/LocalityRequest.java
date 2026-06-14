package com.colectivo.admin.dto.catalog;

import com.colectivo.admin.model.LocalityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocalityRequest {
    @NotBlank
    private String municipalityId;

    @NotBlank
    private String name;

    @NotNull
    private LocalityType type;

    private Boolean active;

    /** Etiqueta/referencia opcional. */
    private String label;
}
