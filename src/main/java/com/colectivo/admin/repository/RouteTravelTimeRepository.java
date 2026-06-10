package com.colectivo.admin.repository;

import com.colectivo.admin.model.RouteTravelTime;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RouteTravelTimeRepository extends MongoRepository<RouteTravelTime, String> {
    Optional<RouteTravelTime> findByOriginLocalityIdAndDestinationLocalityId(String originLocalityId, String destinationLocalityId);
    List<RouteTravelTime> findAllByOrderByCalculatedAtDesc();
}
