package com.colectivo.admin.service;

import com.colectivo.admin.dto.KnownLocationDto;
import com.colectivo.admin.model.KnownLocation;
import com.colectivo.admin.model.User;
import com.colectivo.admin.repository.KnownLocationRepository;
import com.colectivo.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lee la colección compartida {@code known_locations} (escrita por Carpool: el
 * mapa colaborativo de puntos de encuentro resueltos por conductores). Solo
 * lectura para visualizarlos en el panel.
 */
@Service
@RequiredArgsConstructor
public class KnownLocationService {

    private final KnownLocationRepository knownLocationRepository;
    private final UserRepository userRepository;

    public List<KnownLocationDto> list() {
        List<KnownLocation> locations = knownLocationRepository.findAllByOrderByConfirmationsDescUpdatedAtDesc();
        Map<String, User> users = lookupUsers(locations);
        return locations.stream()
                .map(loc -> toDto(loc, users.get(loc.getCreatedByUserId())))
                .collect(Collectors.toList());
    }

    private Map<String, User> lookupUsers(List<KnownLocation> locations) {
        List<String> ids = locations.stream()
                .map(KnownLocation::getCreatedByUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, User> map = new HashMap<>();
        userRepository.findAllById(ids).forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private KnownLocationDto toDto(KnownLocation loc, User creator) {
        return KnownLocationDto.builder()
                .id(loc.getId())
                .lat(loc.getLat())
                .lng(loc.getLng())
                .municipalityName(loc.getMunicipalityName())
                .localityName(loc.getLocalityName())
                .label(loc.getLabel())
                .source(loc.getSource())
                .confirmations(loc.getConfirmations())
                .createdByUserId(loc.getCreatedByUserId())
                .createdByName(creator != null ? creator.getName() : null)
                .createdAt(loc.getCreatedAt())
                .updatedAt(loc.getUpdatedAt())
                .build();
    }
}
