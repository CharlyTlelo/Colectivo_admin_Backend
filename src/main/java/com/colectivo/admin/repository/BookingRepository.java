package com.colectivo.admin.repository;

import com.colectivo.admin.model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    List<Booking> findByTripId(String tripId);

    List<Booking> findByPassengerId(String passengerId);
}
