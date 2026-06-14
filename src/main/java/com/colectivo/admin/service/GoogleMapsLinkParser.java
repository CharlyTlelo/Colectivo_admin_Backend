package com.colectivo.admin.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrae coordenadas (lat/lng) de un link de Google Maps pegado por el admin.
 *
 * Soporta:
 *  - URLs largas con el punto exacto: ...!3d{lat}!4d{lng}
 *  - URLs con centro de vista: .../@{lat},{lng},17z
 *  - Parametros query: ?q={lat},{lng}, ?ll=, ?destination=, ?daddr=
 *  - Coordenadas crudas pegadas: "19.4326, -99.1332"
 *  - Links cortos (maps.app.goo.gl / goo.gl/maps) — se expanden siguiendo el
 *    redirect HTTP para obtener la URL larga con coordenadas.
 *
 * El punto manual ASi obtenido gana siempre sobre el auto-geocode, por eso el
 * objetivo es resolver la coordenada exacta que vio el admin en el mapa.
 */
@Slf4j
@Service
public class GoogleMapsLinkParser {

    // Orden de prioridad: el punto exacto del lugar (!3d!4d) es el mas confiable;
    // luego parametros explicitos de query; al final el centro de vista (@).
    private static final Pattern PLACE_POINT = Pattern.compile("!3d(-?\\d{1,3}\\.\\d+)!4d(-?\\d{1,3}\\.\\d+)");
    private static final Pattern QUERY_POINT = Pattern.compile(
            "[?&](?:q|ll|sll|destination|daddr|saddr)=(-?\\d{1,3}\\.\\d+)(?:%2C|,)\\s*(-?\\d{1,3}\\.\\d+)");
    private static final Pattern VIEW_POINT = Pattern.compile("[@/](-?\\d{1,3}\\.\\d+),(-?\\d{1,3}\\.\\d+)");
    private static final Pattern RAW_POINT = Pattern.compile(
            "^\\s*(-?\\d{1,3}\\.\\d+)\\s*,\\s*(-?\\d{1,3}\\.\\d+)\\s*$");

    private static final Pattern SHORT_LINK = Pattern.compile(
            "https?://(?:maps\\.app\\.goo\\.gl|goo\\.gl/maps|g\\.co/kgs)/", Pattern.CASE_INSENSITIVE);

    private static final int MAX_REDIRECTS = 5;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Resultado del parseo: coordenadas validadas. */
    public record MapPoint(double lat, double lng) {
    }

    public MapPoint parse(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            throw new IllegalArgumentException("Pega un link de Google Maps o coordenadas (lat, lng)");
        }
        String input = rawInput.trim();

        // 1) Coordenadas crudas "lat, lng" sin URL.
        MapPoint raw = match(RAW_POINT, input);
        if (raw != null) {
            return validated(raw);
        }

        // 2) Link corto: expandir siguiendo redirects hasta obtener la URL larga.
        String url = input;
        if (SHORT_LINK.matcher(input).find()) {
            url = expandShortLink(input);
        }

        MapPoint point = extractFromUrl(url);
        if (point == null) {
            throw new IllegalArgumentException(
                    "No se encontraron coordenadas en el link. Abre el lugar en Google Maps y copia el enlace de 'Compartir'.");
        }
        return validated(point);
    }

    private MapPoint extractFromUrl(String url) {
        MapPoint place = match(PLACE_POINT, url);
        if (place != null) {
            return place;
        }
        MapPoint query = match(QUERY_POINT, url);
        if (query != null) {
            return query;
        }
        return match(VIEW_POINT, url);
    }

    /**
     * Sigue los redirects de un link corto manualmente (sin descargar el HTML),
     * leyendo la cabecera Location. En cada salto intenta extraer ya las
     * coordenadas: muchos links cortos redirigen directo a la URL larga.
     */
    private String expandShortLink(String shortUrl) {
        String current = shortUrl;
        try {
            for (int hop = 0; hop < MAX_REDIRECTS; hop++) {
                HttpResponse<Void> response = httpClient.send(
                        HttpRequest.newBuilder(URI.create(current))
                                .timeout(Duration.ofSeconds(6))
                                .header("User-Agent", "Mozilla/5.0 (compatible; ColectivoAdmin/1.0)")
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.discarding());

                int status = response.statusCode();
                if (status < 300 || status >= 400) {
                    return current;
                }
                String location = response.headers().firstValue("location").orElse(null);
                if (location == null || location.isBlank()) {
                    return current;
                }
                current = location.startsWith("http")
                        ? location
                        : URI.create(current).resolve(location).toString();
                if (extractFromUrl(current) != null) {
                    return current;
                }
            }
            return current;
        } catch (Exception ex) {
            log.warn("No se pudo expandir el link corto de Google Maps '{}': {}", shortUrl, ex.toString());
            throw new IllegalArgumentException(
                    "No se pudo abrir el link corto de Google Maps. Pega el enlace largo (el que contiene las coordenadas) o las coordenadas directas.");
        }
    }

    private MapPoint match(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return new MapPoint(Double.parseDouble(matcher.group(1)), Double.parseDouble(matcher.group(2)));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private MapPoint validated(MapPoint point) {
        if (point.lat() < -90 || point.lat() > 90 || point.lng() < -180 || point.lng() > 180) {
            throw new IllegalArgumentException("Las coordenadas del link estan fuera de rango");
        }
        return point;
    }
}
