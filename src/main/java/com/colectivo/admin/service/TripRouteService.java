package com.colectivo.admin.service;

import com.colectivo.admin.dto.TripRouteDto;
import com.colectivo.admin.dto.TripRouteRequest;
import com.colectivo.admin.exception.NotFoundException;
import com.colectivo.admin.model.GeoState;
import com.colectivo.admin.model.Locality;
import com.colectivo.admin.model.Municipality;
import com.colectivo.admin.model.TripRoute;
import com.colectivo.admin.repository.GeoStateRepository;
import com.colectivo.admin.repository.LocalityRepository;
import com.colectivo.admin.repository.MunicipalityRepository;
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
    private final LocalityRepository localityRepository;
    private final MunicipalityRepository municipalityRepository;
    private final GeoStateRepository stateRepository;

    public List<TripRouteDto> list() {
        return repository.findAllByOrderByOriginLocalityNameAscDestinationLocalityNameAscAreaNameAscRouteDestinationAsc().stream()
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
        if (hasText(request.getLocalityOriginId()) && hasText(request.getLocalityDestinationId())) {
            applyLocalityRoute(route, request);
        } else {
            applyLegacyRoute(route, request);
        }
        route.setApproved(request.isApproved());
        route.setActive(request.isActive());
    }

    private void applyLocalityRoute(TripRoute route, TripRouteRequest request) {
        Locality origin = findLocality(request.getLocalityOriginId(), "localityOriginId");
        Locality destination = findLocality(request.getLocalityDestinationId(), "localityDestinationId");
        if (origin.getId().equals(destination.getId())) {
            throw new IllegalArgumentException("La localidad origen y destino no pueden ser la misma.");
        }

        Municipality originMunicipality = findMunicipality(origin.getMunicipalityId());
        Municipality destinationMunicipality = findMunicipality(destination.getMunicipalityId());
        GeoState originState = findState(originMunicipality.getStateId());
        GeoState destinationState = findState(destinationMunicipality.getStateId());

        route.setLocalityOriginId(origin.getId());
        route.setLocalityDestinationId(destination.getId());
        route.setOriginLocalityName(origin.getName());
        route.setOriginMunicipalityName(originMunicipality.getName());
        route.setOriginStateName(originState.getName());
        route.setDestinationLocalityName(destination.getName());
        route.setDestinationMunicipalityName(destinationMunicipality.getName());
        route.setDestinationStateName(destinationState.getName());

        route.setOriginArea(originState.getCode());
        route.setAreaType(originMunicipality.getType().name());
        route.setAreaName(originMunicipality.getName());
        route.setAreaKey(slug(originMunicipality.getName()));
        route.setMeetingPointLabel(origin.getName());
        route.setRouteDestination(destination.getName());
        route.setFinalDestination(destination.getName());
        route.setMeetingPointLat(0);
        route.setMeetingPointLng(0);
        route.setDestinationLat(0);
        route.setDestinationLng(0);
        route.setRouteKey(origin.getId() + "->" + destination.getId());
    }

    private void applyLegacyRoute(TripRoute route, TripRouteRequest request) {
        route.setOriginArea(clean(request.getOriginArea(), "CDMX"));
        route.setAreaType(clean(request.getAreaType(), "ALCALDIA"));
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
    }

    private Locality findLocality(String id, String field) {
        return localityRepository.findById(required(id, field))
                .orElseThrow(() -> new NotFoundException("Locality not found: " + id));
    }

    private Municipality findMunicipality(String id) {
        return municipalityRepository.findById(required(id, "municipalityId"))
                .orElseThrow(() -> new NotFoundException("Municipality not found: " + id));
    }

    private GeoState findState(String id) {
        return stateRepository.findById(required(id, "stateId"))
                .orElseThrow(() -> new NotFoundException("State not found: " + id));
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(clean(value, "sin-ruta"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
