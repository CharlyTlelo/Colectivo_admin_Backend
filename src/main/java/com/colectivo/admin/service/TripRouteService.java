package com.colectivo.admin.service;

import com.colectivo.admin.dto.TripRouteDto;
import com.colectivo.admin.dto.TripRouteRequest;
import com.colectivo.admin.model.TripRoute;
import com.colectivo.admin.repository.TripRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TripRouteService {
    private final TripRouteRepository repository;

    public List<TripRouteDto> list() {
        return repository.findAllByOrderByAreaNameAscRouteDestinationAscFinalDestinationAsc().stream()
                .map(TripRouteDto::from)
                .toList();
    }

    public TripRouteDto create(TripRouteRequest request) {
        Instant now = Instant.now();
        TripRoute route = new TripRoute();
        apply(route, request);
        route.setCreatedAt(now);
        route.setUpdatedAt(now);
        return TripRouteDto.from(repository.save(route));
    }

    public TripRouteDto update(String id, TripRouteRequest request) {
        TripRoute route = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trip route not found: " + id));
        apply(route, request);
        route.setUpdatedAt(Instant.now());
        return TripRouteDto.from(repository.save(route));
    }

    public void delete(String id) {
        repository.deleteById(id);
    }

    private void apply(TripRoute route, TripRouteRequest request) {
        route.setOriginArea(clean(request.getOriginArea(), "CDMX"));
        route.setAreaType(clean(request.getAreaType(), "alcaldia"));
        route.setAreaName(required(request.getAreaName(), "areaName"));
        route.setAreaKey(clean(request.getAreaKey(), slug(route.getAreaName())));
        route.setRouteDestination(required(request.getRouteDestination(), "routeDestination"));
        route.setFinalDestination(required(request.getFinalDestination(), "finalDestination"));
        route.setMeetingPointLabel(required(request.getMeetingPointLabel(), "meetingPointLabel"));
        route.setMeetingPointLat(request.getMeetingPointLat());
        route.setMeetingPointLng(request.getMeetingPointLng());
        route.setDestinationLat(request.getDestinationLat());
        route.setDestinationLng(request.getDestinationLng());
        route.setRouteKey(slug(route.getMeetingPointLabel()) + "->" + slug(route.getRouteDestination()));
        route.setApproved(request.isApproved());
        route.setActive(request.isActive());
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(clean(value, "sin-ruta"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
