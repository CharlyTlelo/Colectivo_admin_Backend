package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuración de banners promocionales por ubicación en la app.
 * Colección compartida con Carpool ({@code mx.colectivo.api.domain.PromoBannerConfig}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "promo_banner_configs")
public class PromoBannerConfig {

    /** Identificador de ubicación, p. ej. {@code driver_home}. */
    @Id
    private String placement;

    /** Segundos entre cambios del carrusel (mínimo 2). */
    private int rotationSeconds;

    private boolean enabled;

    @Builder.Default
    private List<PromoBannerSlide> slides = new ArrayList<>();

    private Instant updatedAt;
}
