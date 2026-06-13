package com.colectivo.admin.repository;

import com.colectivo.admin.model.KnownLocation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface KnownLocationRepository extends MongoRepository<KnownLocation, String> {

    /** Más confirmados primero (los más confiables), luego los más recientes. */
    List<KnownLocation> findAllByOrderByConfirmationsDescUpdatedAtDesc();
}
