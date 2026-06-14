package com.colectivo.admin.controller;

import com.colectivo.admin.dto.catalog.CountryRequest;
import com.colectivo.admin.dto.catalog.CountryResponse;
import com.colectivo.admin.dto.catalog.LocalityMapPointRequest;
import com.colectivo.admin.dto.catalog.LocalityOptionResponse;
import com.colectivo.admin.dto.catalog.LocalityRequest;
import com.colectivo.admin.dto.catalog.LocalityResponse;
import com.colectivo.admin.dto.catalog.MunicipalityRequest;
import com.colectivo.admin.dto.catalog.MunicipalityResponse;
import com.colectivo.admin.dto.catalog.RouteTravelTimeBatchRequest;
import com.colectivo.admin.dto.catalog.RouteTravelTimeBatchResponse;
import com.colectivo.admin.dto.catalog.RouteTravelTimeEstimateResponse;
import com.colectivo.admin.dto.catalog.RouteTravelTimeRequest;
import com.colectivo.admin.dto.catalog.RouteTravelTimeResponse;
import com.colectivo.admin.dto.catalog.RouteTravelTimeSaveRequest;
import com.colectivo.admin.dto.catalog.StateRequest;
import com.colectivo.admin.dto.catalog.StateResponse;
import com.colectivo.admin.service.GeographicCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GeographicCatalogController {
    private final GeographicCatalogService catalogService;

    @GetMapping("/api/v1/catalogs/countries")
    public List<CountryResponse> publicCountries() {
        return catalogService.listCountries(true);
    }

    @GetMapping("/api/v1/catalogs/states")
    public List<StateResponse> publicStates(@RequestParam String countryId) {
        return catalogService.listStates(countryId, true);
    }

    @GetMapping("/api/v1/catalogs/municipalities")
    public List<MunicipalityResponse> publicMunicipalities(@RequestParam String stateId) {
        return catalogService.listMunicipalities(stateId, true);
    }

    @GetMapping("/api/v1/catalogs/localities")
    public List<LocalityResponse> publicLocalities(@RequestParam String municipalityId) {
        return catalogService.listLocalities(municipalityId, true);
    }

    /** Tiempo aproximado para la dirección exacta origen → destino (consumo Carpool). */
    @GetMapping("/api/v1/catalogs/travel-time/estimate")
    public RouteTravelTimeEstimateResponse estimateTravelTime(
            @RequestParam String originLocalityId,
            @RequestParam String destinationLocalityId
    ) {
        return catalogService.getEstimatedTravelTimeByRoute(originLocalityId, destinationLocalityId);
    }

    @GetMapping("/api/v1/admin/catalogs/countries")
    public List<CountryResponse> adminCountries() {
        return catalogService.listCountries(false);
    }

    @PostMapping("/api/v1/admin/catalogs/countries")
    public ResponseEntity<CountryResponse> createCountry(@Valid @RequestBody CountryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createCountry(request));
    }

    @PutMapping("/api/v1/admin/catalogs/countries/{id}")
    public CountryResponse updateCountry(@PathVariable String id, @Valid @RequestBody CountryRequest request) {
        return catalogService.updateCountry(id, request);
    }

    @PatchMapping("/api/v1/admin/catalogs/countries/{id}/deactivate")
    public ResponseEntity<Void> deactivateCountry(@PathVariable String id) {
        catalogService.deactivateCountry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/catalogs/states")
    public List<StateResponse> adminStates(@RequestParam String countryId) {
        return catalogService.listStates(countryId, false);
    }

    @PostMapping("/api/v1/admin/catalogs/states")
    public ResponseEntity<StateResponse> createState(@Valid @RequestBody StateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createState(request));
    }

    @PutMapping("/api/v1/admin/catalogs/states/{id}")
    public StateResponse updateState(@PathVariable String id, @Valid @RequestBody StateRequest request) {
        return catalogService.updateState(id, request);
    }

    @PatchMapping("/api/v1/admin/catalogs/states/{id}/deactivate")
    public ResponseEntity<Void> deactivateState(@PathVariable String id) {
        catalogService.deactivateState(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/catalogs/municipalities")
    public List<MunicipalityResponse> adminMunicipalities(@RequestParam String stateId) {
        return catalogService.listMunicipalities(stateId, false);
    }

    @PostMapping("/api/v1/admin/catalogs/municipalities")
    public ResponseEntity<MunicipalityResponse> createMunicipality(@Valid @RequestBody MunicipalityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createMunicipality(request));
    }

    @PutMapping("/api/v1/admin/catalogs/municipalities/{id}")
    public MunicipalityResponse updateMunicipality(@PathVariable String id, @Valid @RequestBody MunicipalityRequest request) {
        return catalogService.updateMunicipality(id, request);
    }

    @PatchMapping("/api/v1/admin/catalogs/municipalities/{id}/deactivate")
    public ResponseEntity<Void> deactivateMunicipality(@PathVariable String id) {
        catalogService.deactivateMunicipality(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/catalogs/localities")
    public List<LocalityResponse> adminLocalities(@RequestParam String municipalityId) {
        return catalogService.listLocalities(municipalityId, false);
    }

    @PostMapping("/api/v1/admin/catalogs/localities")
    public ResponseEntity<LocalityResponse> createLocality(@Valid @RequestBody LocalityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(catalogService.createLocality(request));
    }

    @PutMapping("/api/v1/admin/catalogs/localities/{id}")
    public LocalityResponse updateLocality(@PathVariable String id, @Valid @RequestBody LocalityRequest request) {
        return catalogService.updateLocality(id, request);
    }

    @PostMapping("/api/v1/admin/catalogs/localities/{id}/travel-time")
    public LocalityResponse calculateLocalityTravelTime(@PathVariable String id) {
        return catalogService.calculateLocalityTravelTime(id);
    }

    @PutMapping("/api/v1/admin/catalogs/localities/{id}/map-point")
    public LocalityResponse setLocalityMapPoint(
            @PathVariable String id,
            @RequestBody LocalityMapPointRequest request
    ) {
        return catalogService.setLocalityMapPoint(
                id, request.getMapsUrl(), request.getLatitude(), request.getLongitude());
    }

    @DeleteMapping("/api/v1/admin/catalogs/localities/{id}/map-point")
    public LocalityResponse clearLocalityMapPoint(@PathVariable String id) {
        return catalogService.clearLocalityMapPoint(id);
    }

    @GetMapping("/api/v1/admin/catalogs/localities/active-options")
    public List<LocalityOptionResponse> activeLocalityOptions(
            @RequestParam String stateId,
            @RequestParam(required = false) String municipalityId
    ) {
        return catalogService.listActiveLocalityOptions(stateId, municipalityId);
    }

    @GetMapping("/api/v1/admin/catalogs/travel-time")
    public List<RouteTravelTimeResponse> listStoredRouteTravelTimes() {
        return catalogService.listStoredRouteTravelTimes();
    }

    @PostMapping("/api/v1/admin/catalogs/travel-time/calculate")
    public RouteTravelTimeResponse calculateRouteTravelTime(@Valid @RequestBody RouteTravelTimeRequest request) {
        return catalogService.calculateRouteTravelTime(
                request.getOriginLocalityId(),
                request.getDestinationLocalityId()
        );
    }

    @PostMapping("/api/v1/admin/catalogs/travel-time/calculate-batch")
    public RouteTravelTimeBatchResponse calculateRouteTravelTimeBatch(
            @Valid @RequestBody RouteTravelTimeBatchRequest request
    ) {
        return catalogService.calculateRouteTravelTimeBatch(
                request.getOriginLocalityIds(),
                request.getDestinationLocalityIds()
        );
    }

    @PostMapping("/api/v1/admin/catalogs/travel-time/save-batch")
    public List<RouteTravelTimeResponse> saveRouteTravelTimes(
            @Valid @RequestBody RouteTravelTimeSaveRequest request
    ) {
        return catalogService.saveRouteTravelTimes(request.getItems());
    }

    @PatchMapping("/api/v1/admin/catalogs/localities/{id}/deactivate")
    public ResponseEntity<Void> deactivateLocality(@PathVariable String id) {
        catalogService.deactivateLocality(id);
        return ResponseEntity.noContent().build();
    }
}
