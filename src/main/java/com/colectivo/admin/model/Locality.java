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

    /**
     * Tiempo IDA mostrado en Admin: municipio→localidad (recalc individual) o
     * salida desde esta localidad hacia el destino del último par guardado (batch).
     */
    private Integer estimatedTravelMinutes;
    /**
     * Tiempo VUELTA mostrado en Admin: localidad→municipio (recalc individual) o
     * regreso hacia esta localidad desde el origen del último par guardado (batch).
     * Carpool consulta route_travel_times por dirección, no estos campos.
     */
    private Integer returnTravelMinutes;
    private Instant travelTimeCalculatedAt;

    /** Coordenadas geocodificadas (cache) para calculos de ruta confiables. */
    private Double latitude;
    private Double longitude;
    private Instant geocodedAt;

    private Instant createdAt;
    private Instant updatedAt;
}
