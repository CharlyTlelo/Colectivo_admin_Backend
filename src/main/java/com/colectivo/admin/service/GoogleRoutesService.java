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
     * Calcula los minutos de manejo entre dos direcciones.
     *
     * @return minutos estimados (redondeados hacia arriba)
     * @throws IllegalStateException si la API key falta, la llamada falla o no hay ruta
     */
    public int drivingMinutes(String originAddress, String destinationAddress) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Google Maps no esta configurado (falta GOOGLE_MAPS_API_KEY)");
        }

        Map<String, Object> body = Map.of(
                "origin", Map.of("address", originAddress),
                "destination", Map.of("address", destinationAddress),
                "travelMode", "DRIVE"
        );

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
        return (int) Math.max(1, Math.ceil(seconds / 60.0));
    }

    /** El API devuelve duraciones como "780s". */
    private long parseDurationSeconds(String duration) {
        try {
            return Long.parseLong(duration.replace("s", "").trim());
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Respuesta de Google Maps invalida: " + duration);
        }
    }

    record Route(String duration, Long distanceMeters) {
    }

    record ComputeRoutesResponse(List<Route> routes) {
    }
}
