package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "localities")
@CompoundIndex(name = "localities_municipality_name_unique", def = "{'municipalityId': 1, 'name': 1}", unique = true)
public class Locality {
    @Id
    private String id;

    private String municipalityId;
    private String name;
    private LocalityType type;
    private boolean active;

    /** Tiempo estimado en auto desde el municipio/alcaldia hasta la localidad (Google Maps). */
    private Integer estimatedTravelMinutes;
    private Instant travelTimeCalculatedAt;

    /** Coordenadas geocodificadas (cache) para calculos de ruta confiables. */
    private Double latitude;
    private Double longitude;
    private Instant geocodedAt;

    private Instant createdAt;
    private Instant updatedAt;
}
