package com.colectivo.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Cliente de Google Geocoding API para resolver localidades a coordenadas.
 * Usa filtro por pais y area administrativa (municipio/alcaldia) para evitar
 * resultados ambiguos (ej. "Centro" existe en cientos de lugares).
 */
@Slf4j
@Service
public class GoogleGeocodingService {

    private static final String GEOCODE_URL = "https://maps.googleapis.com/maps/api/geocode/json";

    private final RestClient restClient = RestClient.create();
    private final String apiKey;

    public GoogleGeocodingService(@Value("${colectivo.maps.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Geocodifica una localidad dentro de un municipio. Primero intenta con
     * filtro estricto de area administrativa; si no hay resultados, reintenta
     * con la direccion completa como texto.
     */
    public GeoPoint geocodeLocality(String localityName, String municipalityName, String stateName, String countryCode) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Google Maps no esta configurado (falta GOOGLE_MAPS_API_KEY)");
        }

        GeoPoint strict = tryGeocode(localityName, "country:" + countryCode + "|administrative_area:" + municipalityName);
        if (strict != null) {
            return strict;
        }

        String fullAddress = String.join(", ", localityName, municipalityName, stateName);
        GeoPoint fallback = tryGeocode(fullAddress, "country:" + countryCode);
        if (fallback != null) {
            log.warn("Geocoding fallback usado para '{}' en '{}'", localityName, municipalityName);
            return fallback;
        }

        throw new IllegalStateException(
                "No se pudo geocodificar la localidad '" + localityName + "' en '" + municipalityName + "'");
    }

    /** Geocodifica un municipio/alcaldia (origen de los tiempos municipio->localidad). */
    public GeoPoint geocodeMunicipality(String municipalityName, String stateName, String countryCode) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Google Maps no esta configurado (falta GOOGLE_MAPS_API_KEY)");
        }
        GeoPoint point = tryGeocode(municipalityName + ", " + stateName, "country:" + countryCode);
        if (point == null) {
            throw new IllegalStateException("No se pudo geocodificar el municipio '" + municipalityName + "'");
        }
        return point;
    }

    private GeoPoint tryGeocode(String address, String components) {
        // build() + encode() + toUri(): pasamos un java.net.URI ya codificado para
        // que RestClient no lo re-codifique (doble encoding corrompe '|' y acentos).
        URI uri = UriComponentsBuilder.fromUriString(GEOCODE_URL)
                .queryParam("address", address)
                .queryParam("components", components)
                .queryParam("region", "mx")
                .queryParam("language", "es")
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri();

        GeocodeResponse response;
        try {
            response = restClient.get().uri(uri).retrieve().body(GeocodeResponse.class);
        } catch (Exception ex) {
            log.error("Google Geocoding API unreachable for '{}'", address, ex);
            throw new IllegalStateException("No se pudo contactar a Google Maps (geocoding)");
        }

        if (response == null || response.status() == null) {
            throw new IllegalStateException("Respuesta invalida de Google Geocoding");
        }
        if ("ZERO_RESULTS".equals(response.status()) || response.results() == null || response.results().isEmpty()) {
            return null;
        }
        if (!"OK".equals(response.status())) {
            log.error("Google Geocoding API error {} for '{}': {}", response.status(), address, response.errorMessage());
            throw new IllegalStateException("Google Geocoding rechazo la solicitud (" + response.status() + ")");
        }

        GeocodeResult result = response.results().get(0);
        Location location = result.geometry().location();
        return new GeoPoint(location.lat(), location.lng(), result.formattedAddress());
    }

    public record GeoPoint(double lat, double lng, String formattedAddress) {
    }

    record GeocodeResponse(String status, List<GeocodeResult> results,
                           @com.fasterxml.jackson.annotation.JsonProperty("error_message") String errorMessage) {
    }

    record GeocodeResult(@com.fasterxml.jackson.annotation.JsonProperty("formatted_address") String formattedAddress,
                         Geometry geometry) {
    }

    record Geometry(Location location) {
    }

    record Location(double lat, double lng) {
    }
}
