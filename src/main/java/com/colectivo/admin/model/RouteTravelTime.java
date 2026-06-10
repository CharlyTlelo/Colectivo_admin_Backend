package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Tiempo de manejo calculado entre dos localidades (Google Routes API).
 * Se guarda un registro por par origen-destino y se actualiza al recalcular.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "route_travel_times")
@CompoundIndex(name = "route_travel_times_pair_unique", def = "{'originLocalityId': 1, 'destinationLocalityId': 1}", unique = true)
public class RouteTravelTime {
    @Id
    private String id;

    private String originLocalityId;
    private String destinationLocalityId;

    /** Etiquetas legibles al momento del calculo (ej. "Centro · Milpa Alta · Ciudad de México"). */
    private String originLabel;
    private String destinationLabel;

    private int estimatedTravelMinutes;
    private double distanceKm;

    private Instant calculatedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
