package com.colectivo.admin.repository;

import com.colectivo.admin.model.GeoState;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface GeoStateRepository extends MongoRepository<GeoState, String> {
    List<GeoState> findAllByOrderByNameAsc();
    List<GeoState> findByCountryIdOrderByNameAsc(String countryId);
    List<GeoState> findByCountryIdAndActiveTrueOrderByNameAsc(String countryId);
    Optional<GeoState> findByCountryIdAndCodeIgnoreCase(String countryId, String code);
    boolean existsByCountryIdAndNameIgnoreCase(String countryId, String name);
    boolean existsByCountryIdAndNameIgnoreCaseAndIdNot(String countryId, String name, String id);
    boolean existsByCountryIdAndCodeIgnoreCase(String countryId, String code);
    boolean existsByCountryIdAndCodeIgnoreCaseAndIdNot(String countryId, String code, String id);
}
