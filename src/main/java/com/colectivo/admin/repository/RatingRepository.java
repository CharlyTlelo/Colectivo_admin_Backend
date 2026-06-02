package com.colectivo.admin.repository;

import com.colectivo.admin.model.Rating;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RatingRepository extends MongoRepository<Rating, String> {

    List<Rating> findByRateeId(String rateeId);

    List<Rating> findByTripId(String tripId);
}
