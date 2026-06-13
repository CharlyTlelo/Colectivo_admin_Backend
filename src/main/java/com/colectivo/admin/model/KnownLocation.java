package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Punto de encuentro geolocalizado resuelto por un conductor en Carpool. Esta
 * colección la ESCRIBE Carpool ({@code mx.colectivo.api.domain.KnownLocation});
 * Admin solo LEE para mostrarlos en un mapa/tabla.
 *
 * <p>El campo GeoJSON {@code location} se omite a propósito: aquí basta con los
 * {@code lat}/{@code lng} desnormalizados que Carpool ya guarda.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "known_locations")
public class KnownLocation {

    @Id
    private String id;

    private double lat;
    private double lng;

    private String municipalityId;
    private String municipalityName;

    @Indexed
    private String localityId;
    private String localityName;

    private String label;
    private String source;
    private int confirmations;

    private String createdByUserId;
    private Instant createdAt;
    private Instant updatedAt;
}
