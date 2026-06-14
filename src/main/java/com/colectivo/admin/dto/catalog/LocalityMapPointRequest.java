package com.colectivo.admin.dto.catalog;

import lombok.Data;

/**
 * Fija el punto manual de una localidad. El admin puede pegar un link de Google
 * Maps (mapsUrl) o, alternativamente, coordenadas directas (latitude/longitude).
 * Si viene mapsUrl tiene prioridad: el backend extrae lat/lng de el.
 */
@Data
public class LocalityMapPointRequest {
    private String mapsUrl;
    private Double latitude;
    private Double longitude;
}
