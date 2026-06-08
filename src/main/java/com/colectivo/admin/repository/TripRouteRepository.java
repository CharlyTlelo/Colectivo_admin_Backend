package com.colectivo.admin.repository;

import com.colectivo.admin.model.TripRoute;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TripRouteRepository extends MongoRepository<TripRoute, String> {
    List<TripRoute> findAllByOrderByOriginLocalityNameAscDestinationLocalityNameAscAreaNameAscRouteDestinationAsc();
}
