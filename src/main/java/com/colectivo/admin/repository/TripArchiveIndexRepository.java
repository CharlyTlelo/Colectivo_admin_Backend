package com.colectivo.admin.repository;

import com.colectivo.admin.model.TripArchiveIndex;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TripArchiveIndexRepository extends MongoRepository<TripArchiveIndex, String> {
    List<TripArchiveIndex> findTop100ByOrderByArchivedAtDesc();
}
