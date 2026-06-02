package com.colectivo.admin.repository;

import com.colectivo.admin.model.Trip;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends MongoRepository<Trip, String> {

    List<Trip> findByDriverId(String driverId);

    List<Trip> findByStatus(String status);
}
