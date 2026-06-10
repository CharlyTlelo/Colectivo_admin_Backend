package com.colectivo.admin.service;

import com.colectivo.admin.dto.catalog.CountryRequest;
import com.colectivo.admin.dto.catalog.CountryResponse;
import com.colectivo.admin.dto.catalog.LocalityOptionResponse;
import com.colectivo.admin.dto.catalog.LocalityRequest;
import com.colectivo.admin.dto.catalog.LocalityResponse;
import com.colectivo.admin.dto.catalog.MunicipalityRequest;
import com.colectivo.admin.dto.catalog.MunicipalityResponse;
import com.colectivo.admin.dto.catalog.RouteTravelTimeBatchResponse;
import com.colectivo.admin.dto.catalog.RouteTravelTimeEstimateResponse;
import com.colectivo.admin.dto.catalog.RouteTravelTimeResponse;
import com.colectivo.admin.dto.catalog.RouteTravelTimeSaveRequest;
import com.colectivo.admin.dto.catalog.StateRequest;
import com.colectivo.admin.dto.catalog.StateResponse;
import com.colectivo.admin.exception.ConflictException;
import com.colectivo.admin.exception.NotFoundException;
import com.colectivo.admin.model.Country;
import com.colectivo.admin.model.GeoState;
import com.colectivo.admin.model.Locality;
import com.colectivo.admin.model.Municipality;
import com.colectivo.admin.model.RouteTravelTime;
import com.colectivo.admin.repository.CountryRepository;
import com.colectivo.admin.repository.GeoStateRepository;
import com.colectivo.admin.repository.LocalityRepository;
import com.colectivo.admin.repository.MunicipalityRepository;
import com.colectivo.admin.repository.RouteTravelTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GeographicCatalogService {

    private static final int MAX_ROUTE_COMBINATIONS = 100;

    private final CountryRepository countryRepository;
    private final GeoStateRepository stateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final LocalityRepository localityRepository;
    private final RouteTravelTimeRepository routeTravelTimeRepository;
    private final GoogleRoutesService googleRoutesService;
    private final GoogleGeocodingService googleGeocodingService;

    public List<CountryResponse> listCountries(boolean activeOnly) {
        return (activeOnly ? countryRepository.findByActiveTrueOrderByNameAsc() : countryRepository.findAllByOrderByNameAsc())
                .stream()
                .map(CountryResponse::from)
                .toList();
    }

    public List<StateResponse> listStates(String countryId, boolean activeOnly) {
        required(countryId, "countryId");
        return (activeOnly
                ? stateRepository.findByCountryIdAndActiveTrueOrderByNameAsc(countryId)
                : stateRepository.findByCountryIdOrderByNameAsc(countryId))
                .stream()
                .map(StateResponse::from)
                .toList();
    }

    public List<MunicipalityResponse> listMunicipalities(String stateId, boolean activeOnly) {
        required(stateId, "stateId");
        return (activeOnly
                ? municipalityRepository.findByStateIdAndActiveTrueOrderByNameAsc(stateId)
                : municipalityRepository.findByStateIdOrderByNameAsc(stateId))
                .stream()
                .map(MunicipalityResponse::from)
                .toList();
    }

    public List<LocalityResponse> listLocalities(String municipalityId, boolean activeOnly) {
        required(municipalityId, "municipalityId");
        return (activeOnly
                ? localityRepository.findByMunicipalityIdAndActiveTrueOrderByNameAsc(municipalityId)
                : localityRepository.findByMunicipalityIdOrderByNameAsc(municipalityId))
                .stream()
                .map(LocalityResponse::from)
                .toList();
    }

    public CountryResponse createCountry(CountryRequest request) {
        String code = code(request.getCode());
        if (countryRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Country code already exists: " + code);
        }
        Instant now = Instant.now();
        Country country = Country.builder()
                .name(required(request.getName(), "name"))
                .code(code)
                .active(defaultActive(request.getActive()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return CountryResponse.from(countryRepository.save(country));
    }

    public CountryResponse updateCountry(String id, CountryRequest request) {
        Country country = findCountry(id);
        String code = code(request.getCode());
        if (countryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("Country code already exists: " + code);
        }
        country.setName(required(request.getName(), "name"));
        country.setCode(code);
        country.setActive(defaultActive(request.getActive()));
        country.setUpdatedAt(Instant.now());
        return CountryResponse.from(countryRepository.save(country));
    }

    public void deactivateCountry(String id) {
        Country country = findCountry(id);
        country.setActive(false);
        country.setUpdatedAt(Instant.now());
        countryRepository.save(country);
    }

    public StateResponse createState(StateRequest request) {
        Country country = findCountry(request.getCountryId());
        String name = required(request.getName(), "name");
        String code = code(request.getCode());
        ensureStateUnique(country.getId(), name, code, null);
        Instant now = Instant.now();
        GeoState state = GeoState.builder()
                .countryId(country.getId())
                .name(name)
                .code(code)
                .active(defaultActive(request.getActive()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return StateResponse.from(stateRepository.save(state));
    }

    public StateResponse updateState(String id, StateRequest request) {
        GeoState state = findState(id);
        Country country = findCountry(request.getCountryId());
        String name = required(request.getName(), "name");
        String code = code(request.getCode());
        ensureStateUnique(country.getId(), name, code, id);
        state.setCountryId(country.getId());
        state.setName(name);
        state.setCode(code);
        state.setActive(defaultActive(request.getActive()));
        state.setUpdatedAt(Instant.now());
        return StateResponse.from(stateRepository.save(state));
    }

    public void deactivateState(String id) {
        GeoState state = findState(id);
        state.setActive(false);
        state.setUpdatedAt(Instant.now());
        stateRepository.save(state);
    }

    public MunicipalityResponse createMunicipality(MunicipalityRequest request) {
        GeoState state = findState(request.getStateId());
        String name = required(request.getName(), "name");
        ensureMunicipalityUnique(state.getId(), name, null);
        Instant now = Instant.now();
        Municipality municipality = Municipality.builder()
                .stateId(state.getId())
                .name(name)
                .type(request.getType())
                .active(defaultActive(request.getActive()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return MunicipalityResponse.from(municipalityRepository.save(municipality));
    }

    public MunicipalityResponse updateMunicipality(String id, MunicipalityRequest request) {
        Municipality municipality = findMunicipality(id);
        GeoState state = findState(request.getStateId());
        String name = required(request.getName(), "name");
        ensureMunicipalityUnique(state.getId(), name, id);
        municipality.setStateId(state.getId());
        municipality.setName(name);
        municipality.setType(request.getType());
        municipality.setActive(defaultActive(request.getActive()));
        municipality.setUpdatedAt(Instant.now());
        return MunicipalityResponse.from(municipalityRepository.save(municipality));
    }

    public void deactivateMunicipality(String id) {
        Municipality municipality = findMunicipality(id);
        municipality.setActive(false);
        municipality.setUpdatedAt(Instant.now());
        municipalityRepository.save(municipality);
    }

    public LocalityResponse createLocality(LocalityRequest request) {
        Municipality municipality = findMunicipality(request.getMunicipalityId());
        String name = required(request.getName(), "name");
        ensureLocalityUnique(municipality.getId(), name, null);
        Instant now = Instant.now();
        Locality locality = Locality.builder()
                .municipalityId(municipality.getId())
                .name(name)
                .type(request.getType())
                .active(defaultActive(request.getActive()))
                .createdAt(now)
                .updatedAt(now)
                .build();
        return LocalityResponse.from(localityRepository.save(locality));
    }

    public LocalityResponse updateLocality(String id, LocalityRequest request) {
        Locality locality = findLocality(id);
        Municipality municipality = findMunicipality(request.getMunicipalityId());
        String name = required(request.getName(), "name");
        ensureLocalityUnique(municipality.getId(), name, id);
        locality.setMunicipalityId(municipality.getId());
        locality.setName(name);
        locality.setType(request.getType());
        locality.setActive(defaultActive(request.getActive()));
        locality.setUpdatedAt(Instant.now());
        return LocalityResponse.from(localityRepository.save(locality));
    }

    public List<LocalityOptionResponse> listActiveLocalityOptions(String stateId, String municipalityId) {
        required(stateId, "stateId");
        GeoState state = findState(stateId);
        Country country = findCountry(state.getCountryId());

        List<Municipality> municipalities;
        if (municipalityId != null && !municipalityId.isBlank()) {
            Municipality municipality = findMunicipality(municipalityId);
            if (!state.getId().equals(municipality.getStateId())) {
                throw new IllegalArgumentException("El municipio no pertenece a la entidad seleccionada");
            }
            municipalities = List.of(municipality);
        } else {
            municipalities = municipalityRepository.findByStateIdAndActiveTrueOrderByNameAsc(stateId);
        }

        List<LocalityOptionResponse> options = new ArrayList<>();
        for (Municipality municipality : municipalities) {
            if (!municipality.isActive()) {
                continue;
            }
            for (Locality locality : localityRepository.findByMunicipalityIdAndActiveTrueOrderByNameAsc(municipality.getId())) {
                options.add(toLocalityOption(locality, municipality, state, country));
            }
        }

        options.sort(Comparator.comparing(LocalityOptionResponse::label, String.CASE_INSENSITIVE_ORDER));
        return options;
    }

    public RouteTravelTimeResponse calculateRouteTravelTime(String originLocalityId, String destinationLocalityId) {
        if (originLocalityId.equals(destinationLocalityId)) {
            throw new IllegalArgumentException("El origen y el destino deben ser localidades distintas");
        }

        Locality origin = findActiveLocality(originLocalityId);
        Locality destination = findActiveLocality(destinationLocalityId);

        LocalityContext originContext = resolveLocalityContext(origin);
        LocalityContext destinationContext = resolveLocalityContext(destination);

        GoogleGeocodingService.GeoPoint originPoint = resolveLocalityCoordinates(origin);
        GoogleGeocodingService.GeoPoint destinationPoint = resolveLocalityCoordinates(destination);

        // IDA: origen → destino (cálculo existente; no se modifica).
        GoogleRoutesService.DrivingRouteResult route = googleRoutesService.computeDrivingRoute(
                originPoint.lat(), originPoint.lng(),
                destinationPoint.lat(), destinationPoint.lng()
        );
        // VUELTA: destino → origen (cálculo adicional del mismo botón).
        GoogleRoutesService.DrivingRouteResult routeBack = googleRoutesService.computeDrivingRoute(
                destinationPoint.lat(), destinationPoint.lng(),
                originPoint.lat(), originPoint.lng()
        );

        double distanceKm = Math.round(route.distanceMeters() / 100.0) / 10.0;
        double returnDistanceKm = Math.round(routeBack.distanceMeters() / 100.0) / 10.0;

        // Solo previsualización: NO persiste. El guardado (y el reflejo en las
        // localidades) ocurre al pulsar "Guardar registro" → saveRouteTravelTimes.
        return RouteTravelTimeResponse.builder()
                .originLocalityId(origin.getId())
                .destinationLocalityId(destination.getId())
                .originLabel(originContext.label())
                .destinationLabel(destinationContext.label())
                .estimatedTravelMinutes(route.minutes())
                .distanceKm(distanceKm)
                .returnTravelMinutes(routeBack.minutes())
                .returnDistanceKm(returnDistanceKm)
                .calculatedAt(Instant.now())
                .build();
    }

    /**
     * Persiste tiempos ya calculados (botón "Guardar registro"): guarda el par
     * origen-destino en route_travel_times y refleja ida/vuelta en AMBAS
     * localidades. No vuelve a llamar a Google: usa los minutos/distancias del
     * cálculo previo.
     */
    public List<RouteTravelTimeResponse> saveRouteTravelTimes(List<RouteTravelTimeSaveRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("No hay rutas calculadas para guardar");
        }
        List<RouteTravelTimeResponse> out = new ArrayList<>(items.size());
        for (RouteTravelTimeSaveRequest.Item item : items) {
            if (item.getOriginLocalityId().equals(item.getDestinationLocalityId())) {
                continue;
            }
            Locality origin = findActiveLocality(item.getOriginLocalityId());
            Locality destination = findActiveLocality(item.getDestinationLocalityId());
            LocalityContext originContext = resolveLocalityContext(origin);
            LocalityContext destinationContext = resolveLocalityContext(destination);

            RouteTravelTime saved = saveRouteTravelTime(
                    origin.getId(), destination.getId(),
                    originContext.label(), destinationContext.label(),
                    item.getEstimatedTravelMinutes(), item.getDistanceKm(),
                    item.getReturnTravelMinutes(), item.getReturnDistanceKm()
            );

            // Reflejar en AMBAS localidades (ida = salir de ella, vuelta = regresar):
            //  - origen:  ida = origen→destino,  vuelta = destino→origen
            //  - destino: ida = destino→origen,  vuelta = origen→destino
            applyLocalityRouteTimes(origin, item.getEstimatedTravelMinutes(), item.getReturnTravelMinutes(), saved.getCalculatedAt());
            applyLocalityRouteTimes(destination, item.getReturnTravelMinutes(), item.getEstimatedTravelMinutes(), saved.getCalculatedAt());

            out.add(RouteTravelTimeResponse.builder()
                    .originLocalityId(origin.getId())
                    .destinationLocalityId(destination.getId())
                    .originLabel(originContext.label())
                    .destinationLabel(destinationContext.label())
                    .estimatedTravelMinutes(item.getEstimatedTravelMinutes())
                    .distanceKm(item.getDistanceKm())
                    .returnTravelMinutes(item.getReturnTravelMinutes())
                    .returnDistanceKm(item.getReturnDistanceKm())
                    .calculatedAt(saved.getCalculatedAt())
                    .build());
        }
        return out;
    }

    /** Persiste los tiempos ida/vuelta en una localidad (ida = salida, vuelta = regreso). */
    private void applyLocalityRouteTimes(Locality locality, int outboundMinutes, int returnMinutes, Instant now) {
        locality.setEstimatedTravelMinutes(outboundMinutes);
        locality.setReturnTravelMinutes(returnMinutes);
        locality.setTravelTimeCalculatedAt(now);
        locality.setUpdatedAt(now);
        localityRepository.save(locality);
    }

    /** Guarda (o actualiza) el calculo para el par origen-destino; lo usaremos despues para tarifas/planeacion. */
    private RouteTravelTime saveRouteTravelTime(
            String originLocalityId,
            String destinationLocalityId,
            String originLabel,
            String destinationLabel,
            int minutes,
            double distanceKm,
            int returnMinutes,
            double returnDistanceKm
    ) {
        Instant now = Instant.now();
        RouteTravelTime record = routeTravelTimeRepository
                .findByOriginLocalityIdAndDestinationLocalityId(originLocalityId, destinationLocalityId)
                .orElseGet(() -> RouteTravelTime.builder()
                        .originLocalityId(originLocalityId)
                        .destinationLocalityId(destinationLocalityId)
                        .createdAt(now)
                        .build());
        record.setOriginLabel(originLabel);
        record.setDestinationLabel(destinationLabel);
        record.setEstimatedTravelMinutes(minutes);
        record.setDistanceKm(distanceKm);
        record.setReturnTravelMinutes(returnMinutes);
        record.setReturnDistanceKm(returnDistanceKm);
        record.setCalculatedAt(now);
        record.setUpdatedAt(now);
        return routeTravelTimeRepository.save(record);
    }

    private static final String TRAVEL_TIME_UNAVAILABLE =
            "Tiempo aproximado no disponible para esta ruta.";

    /**
     * Tiempo de manejo para la dirección exacta solicitada (origen → destino).
     * Busca en route_travel_times: si el par guardado coincide con la consulta
     * devuelve IDA; si existe el par inverso devuelve VUELTA (destino → origen
     * del registro canónico). No intercambia IDA por VUELTA sin validar el registro.
     */
    public RouteTravelTimeEstimateResponse getEstimatedTravelTimeByRoute(
            String originLocalityId,
            String destinationLocalityId
    ) {
        if (originLocalityId == null || destinationLocalityId == null
                || originLocalityId.isBlank() || destinationLocalityId.isBlank()) {
            throw new IllegalArgumentException("originLocalityId y destinationLocalityId son obligatorios");
        }
        if (originLocalityId.equals(destinationLocalityId)) {
            throw new IllegalArgumentException("El origen y el destino deben ser localidades distintas");
        }

        Optional<RouteTravelTime> direct = routeTravelTimeRepository
                .findByOriginLocalityIdAndDestinationLocalityId(originLocalityId, destinationLocalityId);
        if (direct.isPresent()) {
            RouteTravelTime record = direct.get();
            return RouteTravelTimeEstimateResponse.builder()
                    .originLocalityId(originLocalityId)
                    .destinationLocalityId(destinationLocalityId)
                    .available(true)
                    .estimatedTravelMinutes(record.getEstimatedTravelMinutes())
                    .distanceKm(record.getDistanceKm())
                    .build();
        }

        Optional<RouteTravelTime> reverse = routeTravelTimeRepository
                .findByOriginLocalityIdAndDestinationLocalityId(destinationLocalityId, originLocalityId);
        if (reverse.isPresent()) {
            RouteTravelTime record = reverse.get();
            return RouteTravelTimeEstimateResponse.builder()
                    .originLocalityId(originLocalityId)
                    .destinationLocalityId(destinationLocalityId)
                    .available(true)
                    .estimatedTravelMinutes(record.getReturnTravelMinutes())
                    .distanceKm(record.getReturnDistanceKm())
                    .build();
        }

        return RouteTravelTimeEstimateResponse.builder()
                .originLocalityId(originLocalityId)
                .destinationLocalityId(destinationLocalityId)
                .available(false)
                .estimatedTravelMinutes(null)
                .distanceKm(null)
                .message(TRAVEL_TIME_UNAVAILABLE)
                .build();
    }

    /** Lista todos los tiempos de ruta guardados (mas recientes primero). */
    public List<RouteTravelTimeResponse> listStoredRouteTravelTimes() {
        return routeTravelTimeRepository.findAllByOrderByCalculatedAtDesc().stream()
                .map(record -> RouteTravelTimeResponse.builder()
                        .originLocalityId(record.getOriginLocalityId())
                        .destinationLocalityId(record.getDestinationLocalityId())
                        .originLabel(record.getOriginLabel())
                        .destinationLabel(record.getDestinationLabel())
                        .estimatedTravelMinutes(record.getEstimatedTravelMinutes())
                        .distanceKm(record.getDistanceKm())
                        .returnTravelMinutes(record.getReturnTravelMinutes())
                        .returnDistanceKm(record.getReturnDistanceKm())
                        .calculatedAt(record.getCalculatedAt())
                        .build())
                .toList();
    }

    public RouteTravelTimeBatchResponse calculateRouteTravelTimeBatch(
            List<String> originLocalityIds,
            List<String> destinationLocalityIds
    ) {
        Set<String> origins = distinctIds(originLocalityIds, "originLocalityIds");
        Set<String> destinations = distinctIds(destinationLocalityIds, "destinationLocalityIds");

        int skippedSamePoint = 0;
        int requested = 0;
        for (String originId : origins) {
            for (String destinationId : destinations) {
                if (originId.equals(destinationId)) {
                    skippedSamePoint++;
                    continue;
                }
                requested++;
            }
        }

        if (requested == 0) {
            throw new IllegalArgumentException("Selecciona al menos un par origen-destino distinto");
        }
        if (requested > MAX_ROUTE_COMBINATIONS) {
            throw new IllegalArgumentException(
                    "Demasiadas combinaciones (" + requested + "). Maximo permitido: " + MAX_ROUTE_COMBINATIONS
            );
        }

        List<RouteTravelTimeResponse> results = new ArrayList<>(requested);
        for (String originId : origins) {
            for (String destinationId : destinations) {
                if (originId.equals(destinationId)) {
                    continue;
                }
                results.add(calculateRouteTravelTime(originId, destinationId));
            }
        }

        return RouteTravelTimeBatchResponse.builder()
                .results(results)
                .requestedCombinations(requested)
                .skippedSamePointCombinations(skippedSamePoint)
                .build();
    }

    private Set<String> distinctIds(List<String> ids, String fieldName) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " no puede estar vacio");
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException(fieldName + " contiene ids invalidos");
            }
            distinct.add(id.trim());
        }
        return distinct;
    }

    /**
     * Calcula (o recalcula manualmente) el tiempo estimado de manejo desde el
     * municipio/alcaldia hasta la localidad usando Google Maps y lo persiste.
     */
    public LocalityResponse calculateLocalityTravelTime(String id) {
        Locality locality = findLocality(id);
        Municipality municipality = findMunicipality(locality.getMunicipalityId());
        GeoState state = findState(municipality.getStateId());
        Country country = findCountry(state.getCountryId());

        GoogleGeocodingService.GeoPoint originPoint = googleGeocodingService.geocodeMunicipality(
                municipality.getName(), state.getName(), countryCode(country));
        GoogleGeocodingService.GeoPoint destinationPoint = resolveLocalityCoordinates(locality);

        // IDA: municipio/alcaldia → localidad.
        int minutes = googleRoutesService.computeDrivingRoute(
                originPoint.lat(), originPoint.lng(),
                destinationPoint.lat(), destinationPoint.lng()
        ).minutes();

        // VUELTA: localidad → municipio/alcaldia. El tiempo difiere por sentido
        // (tráfico/topología vial), así que se calcula como ruta independiente.
        int returnMinutes = googleRoutesService.computeDrivingRoute(
                destinationPoint.lat(), destinationPoint.lng(),
                originPoint.lat(), originPoint.lng()
        ).minutes();

        Instant now = Instant.now();
        locality.setEstimatedTravelMinutes(minutes);
        locality.setReturnTravelMinutes(returnMinutes);
        locality.setTravelTimeCalculatedAt(now);
        locality.setUpdatedAt(now);
        return LocalityResponse.from(localityRepository.save(locality));
    }

    public void deactivateLocality(String id) {
        Locality locality = findLocality(id);
        locality.setActive(false);
        locality.setUpdatedAt(Instant.now());
        localityRepository.save(locality);
    }

    private Country findCountry(String id) {
        return countryRepository.findById(required(id, "countryId"))
                .orElseThrow(() -> new NotFoundException("Country not found: " + id));
    }

    private GeoState findState(String id) {
        return stateRepository.findById(required(id, "stateId"))
                .orElseThrow(() -> new NotFoundException("State not found: " + id));
    }

    private Municipality findMunicipality(String id) {
        return municipalityRepository.findById(required(id, "municipalityId"))
                .orElseThrow(() -> new NotFoundException("Municipality not found: " + id));
    }

    private Locality findLocality(String id) {
        return localityRepository.findById(required(id, "localityId"))
                .orElseThrow(() -> new NotFoundException("Locality not found: " + id));
    }

    private Locality findActiveLocality(String id) {
        Locality locality = findLocality(id);
        if (!locality.isActive()) {
            throw new IllegalArgumentException("La localidad no esta activa: " + locality.getName());
        }
        return locality;
    }

    private LocalityContext resolveLocalityContext(Locality locality) {
        Municipality municipality = findMunicipality(locality.getMunicipalityId());
        GeoState state = findState(municipality.getStateId());
        Country country = findCountry(state.getCountryId());
        return new LocalityContext(
                toLocalityOption(locality, municipality, state, country).label()
        );
    }

    /**
     * Devuelve las coordenadas de la localidad. Si no estan cacheadas, las
     * geocodifica (filtrando por municipio) y las persiste para usos futuros.
     */
    private GoogleGeocodingService.GeoPoint resolveLocalityCoordinates(Locality locality) {
        if (locality.getLatitude() != null && locality.getLongitude() != null) {
            return new GoogleGeocodingService.GeoPoint(locality.getLatitude(), locality.getLongitude(), null);
        }

        Municipality municipality = findMunicipality(locality.getMunicipalityId());
        GeoState state = findState(municipality.getStateId());
        Country country = findCountry(state.getCountryId());

        GoogleGeocodingService.GeoPoint point = googleGeocodingService.geocodeLocality(
                locality.getName(), municipality.getName(), state.getName(), countryCode(country));

        locality.setLatitude(point.lat());
        locality.setLongitude(point.lng());
        locality.setGeocodedAt(Instant.now());
        locality.setUpdatedAt(Instant.now());
        localityRepository.save(locality);
        return point;
    }

    private String countryCode(Country country) {
        return country.getCode() == null || country.getCode().isBlank() ? "MX" : country.getCode().trim();
    }

    private LocalityOptionResponse toLocalityOption(
            Locality locality,
            Municipality municipality,
            GeoState state,
            Country country
    ) {
        String label = String.join(" · ", locality.getName(), municipality.getName(), state.getName());
        return LocalityOptionResponse.builder()
                .id(locality.getId())
                .name(locality.getName())
                .municipalityName(municipality.getName())
                .stateName(state.getName())
                .type(locality.getType())
                .label(label)
                .build();
    }

    private record LocalityContext(String label) {
    }

    private void ensureStateUnique(String countryId, String name, String code, String currentId) {
        boolean duplicatedName = currentId == null
                ? stateRepository.existsByCountryIdAndNameIgnoreCase(countryId, name)
                : stateRepository.existsByCountryIdAndNameIgnoreCaseAndIdNot(countryId, name, currentId);
        if (duplicatedName) {
            throw new ConflictException("State name already exists in this country: " + name);
        }
        boolean duplicatedCode = currentId == null
                ? stateRepository.existsByCountryIdAndCodeIgnoreCase(countryId, code)
                : stateRepository.existsByCountryIdAndCodeIgnoreCaseAndIdNot(countryId, code, currentId);
        if (duplicatedCode) {
            throw new ConflictException("State code already exists in this country: " + code);
        }
    }

    private void ensureMunicipalityUnique(String stateId, String name, String currentId) {
        boolean duplicated = currentId == null
                ? municipalityRepository.existsByStateIdAndNameIgnoreCase(stateId, name)
                : municipalityRepository.existsByStateIdAndNameIgnoreCaseAndIdNot(stateId, name, currentId);
        if (duplicated) {
            throw new ConflictException("Municipality or alcaldia already exists in this state: " + name);
        }
    }

    private void ensureLocalityUnique(String municipalityId, String name, String currentId) {
        boolean duplicated = currentId == null
                ? localityRepository.existsByMunicipalityIdAndNameIgnoreCase(municipalityId, name)
                : localityRepository.existsByMunicipalityIdAndNameIgnoreCaseAndIdNot(municipalityId, name, currentId);
        if (duplicated) {
            throw new ConflictException("Locality already exists in this municipality or alcaldia: " + name);
        }
    }

    private boolean defaultActive(Boolean active) {
        return active == null || active;
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private String code(String value) {
        return required(value, "code").toUpperCase(Locale.ROOT);
    }
}
