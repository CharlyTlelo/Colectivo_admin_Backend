package com.colectivo.admin.service;

import com.colectivo.admin.dto.catalog.CountryRequest;
import com.colectivo.admin.dto.catalog.CountryResponse;
import com.colectivo.admin.dto.catalog.LocalityRequest;
import com.colectivo.admin.dto.catalog.LocalityResponse;
import com.colectivo.admin.dto.catalog.MunicipalityRequest;
import com.colectivo.admin.dto.catalog.MunicipalityResponse;
import com.colectivo.admin.dto.catalog.StateRequest;
import com.colectivo.admin.dto.catalog.StateResponse;
import com.colectivo.admin.exception.ConflictException;
import com.colectivo.admin.exception.NotFoundException;
import com.colectivo.admin.model.Country;
import com.colectivo.admin.model.GeoState;
import com.colectivo.admin.model.Locality;
import com.colectivo.admin.model.Municipality;
import com.colectivo.admin.repository.CountryRepository;
import com.colectivo.admin.repository.GeoStateRepository;
import com.colectivo.admin.repository.LocalityRepository;
import com.colectivo.admin.repository.MunicipalityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class GeographicCatalogService {
    private final CountryRepository countryRepository;
    private final GeoStateRepository stateRepository;
    private final MunicipalityRepository municipalityRepository;
    private final LocalityRepository localityRepository;
    private final GoogleRoutesService googleRoutesService;

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

    /**
     * Calcula (o recalcula manualmente) el tiempo estimado de manejo desde el
     * municipio/alcaldia hasta la localidad usando Google Maps y lo persiste.
     */
    public LocalityResponse calculateLocalityTravelTime(String id) {
        Locality locality = findLocality(id);
        Municipality municipality = findMunicipality(locality.getMunicipalityId());
        GeoState state = findState(municipality.getStateId());
        Country country = findCountry(state.getCountryId());

        String origin = String.join(", ", municipality.getName(), state.getName(), country.getName());
        String destination = String.join(", ", locality.getName(), municipality.getName(), state.getName(), country.getName());

        int minutes = googleRoutesService.drivingMinutes(origin, destination);

        Instant now = Instant.now();
        locality.setEstimatedTravelMinutes(minutes);
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
