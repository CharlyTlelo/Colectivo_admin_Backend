package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromoBannerSlide {

    private String id;

    /** URL https o data URL (base64) de la imagen del banner. */
    private String imageUrl;

    /**
     * Comportamiento al tocar el banner:
     * {@code none} solo imagen, {@code link} abre URL externa,
     * {@code internal} navega a ruta interna de la app.
     */
    private String actionType;

    /** URL externa cuando actionType es {@code link}. */
    private String linkUrl;

    /** Ruta interna (p. ej. {@code /search}) cuando actionType es {@code internal}. */
    private String internalRoute;

    private int sortOrder;

    private boolean enabled;
}
