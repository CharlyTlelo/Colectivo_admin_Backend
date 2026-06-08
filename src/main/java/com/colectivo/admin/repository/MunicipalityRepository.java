package com.colectivo.admin.repository;

import com.colectivo.admin.model.Municipality;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MunicipalityRepository extends MongoRepository<Municipality, String> {
    List<Municipality> findAllByOrderByNameAsc();
    List<Municipality> findByStateIdOrderByNameAsc(String stateId);
    List<Municipality> findByStateIdAndActiveTrueOrderByNameAsc(String stateId);
    Optional<Municipality> findByStateIdAndNameIgnoreCase(String stateId, String name);
    boolean existsByStateIdAndNameIgnoreCase(String stateId, String name);
    boolean existsByStateIdAndNameIgnoreCaseAndIdNot(String stateId, String name, String id);
}
