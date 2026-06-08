package com.colectivo.admin.repository;

import com.colectivo.admin.model.Locality;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface LocalityRepository extends MongoRepository<Locality, String> {
    List<Locality> findAllByOrderByNameAsc();
    List<Locality> findByMunicipalityIdOrderByNameAsc(String municipalityId);
    List<Locality> findByMunicipalityIdAndActiveTrueOrderByNameAsc(String municipalityId);
    Optional<Locality> findByMunicipalityIdAndNameIgnoreCase(String municipalityId, String name);
    boolean existsByMunicipalityIdAndNameIgnoreCase(String municipalityId, String name);
    boolean existsByMunicipalityIdAndNameIgnoreCaseAndIdNot(String municipalityId, String name, String id);
}
