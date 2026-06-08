package com.colectivo.admin.repository;

import com.colectivo.admin.model.Country;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends MongoRepository<Country, String> {
    List<Country> findAllByOrderByNameAsc();
    List<Country> findByActiveTrueOrderByNameAsc();
    Optional<Country> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, String id);
}
