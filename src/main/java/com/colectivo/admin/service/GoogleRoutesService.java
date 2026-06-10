package com.colectivo.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Cliente de Google Routes API (routes.googleapis.com) para calcular
 * tiempos de manejo entre dos direcciones de texto.
 */
@Slf4j
@Service
public class GoogleRoutesService {

    private static final String COMPUTE_ROUTES_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";

    private final RestClient restClient = RestClient.create();
    private final String apiKey;

    public GoogleRoutesService(@Value("${colectivo.maps.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Calcula la ruta en auto entre dos coordenadas. Usamos lat/lng (no texto)
     * porque Google geocodifica de forma ambigua nombres como "Centro" o
     * localidades dentro de una alcaldia, colapsando origen y destino.
     */
    public DrivingRouteResult computeDrivingRoute(double originLat, double originLng,
                                                  double destinationLat, double destinationLng) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Google Maps no esta configurado (falta GOOGLE_MAPS_API_KEY)");
        }

        Map<String, Object> body = Map.of(
                "origin", latLngWaypoint(originLat, originLng),
                "destination", latLngWaypoint(destinationLat, destinationLng),
                "travelMode", "DRIVE",
                "routingPreference", "TRAFFIC_AWARE",
                "languageCode", "es-MX",
                "regionCode", "MX"
        );
        String originAddress = originLat + "," + originLng;
        String destinationAddress = destinationLat + "," + destinationLng;

        ComputeRoutesResponse response;
        try {
            response = restClient.post()
                    .uri(COMPUTE_ROUTES_URL)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "routes.duration,routes.distanceMeters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(ComputeRoutesResponse.class);
        } catch (RestClientResponseException ex) {
            log.error("Google Routes API error {} for '{}' -> '{}': {}",
                    ex.getStatusCode(), originAddress, destinationAddress, ex.getResponseBodyAsString());
            throw new IllegalStateException("Google Maps rechazo la solicitud de ruta (" + ex.getStatusCode().value() + ")");
        } catch (Exception ex) {
            log.error("Google Routes API unreachable for '{}' -> '{}'", originAddress, destinationAddress, ex);
            throw new IllegalStateException("No se pudo contactar a Google Maps");
        }

        if (response == null || response.routes() == null || response.routes().isEmpty()
                || response.routes().get(0).duration() == null) {
            throw new IllegalStateException("Google Maps no encontro ruta entre '" + originAddress + "' y '" + destinationAddress + "'");
        }

        long seconds = parseDurationSeconds(response.routes().get(0).duration());
        int minutes = (int) Math.max(1, Math.ceil(seconds / 60.0));
        Long distanceMeters = response.routes().get(0).distanceMeters();
        return new DrivingRouteResult(minutes, distanceMeters != null ? distanceMeters : 0L);
    }

    private Map<String, Object> latLngWaypoint(double lat, double lng) {
        return Map.of("location", Map.of("latLng", Map.of("latitude", lat, "longitude", lng)));
    }

    private long parseDurationSeconds(String duration) {
        try {
            return Long.parseLong(duration.replace("s", "").trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Respuesta de Google Maps invalida: " + duration);
        }
    }

    public record DrivingRouteResult(int minutes, long distanceMeters) {
    }

    record Route(String duration, Long distanceMeters) {
    }

    record ComputeRoutesResponse(List<Route> routes) {
    }
}
