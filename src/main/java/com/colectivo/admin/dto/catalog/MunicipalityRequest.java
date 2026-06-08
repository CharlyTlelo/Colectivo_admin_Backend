package com.colectivo.admin.dto.catalog;

import com.colectivo.admin.model.MunicipalityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MunicipalityRequest {
    @NotBlank
    private String stateId;

    @NotBlank
    private String name;

    @NotNull
    private MunicipalityType type;

    private Boolean active;
}
