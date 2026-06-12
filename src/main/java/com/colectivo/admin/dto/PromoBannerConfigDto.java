package com.colectivo.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoBannerConfigDto {

    @NotBlank
    private String placement;

    @Min(2)
    @Max(120)
    private int rotationSeconds;

    private boolean enabled;

    @Valid
    @NotNull
    private List<PromoBannerSlideDto> slides;

    private Instant updatedAt;

    /** Recomendación mostrada en el panel de administración. */
    public static final String RECOMMENDED_SIZE_LABEL = "1200 × 530 px";
    public static final String RECOMMENDED_RATIO_LABEL = "proporción ~2.3:1 (ancho completo del teléfono)";
}
