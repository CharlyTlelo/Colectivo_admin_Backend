package com.colectivo.admin.repository;

import com.colectivo.admin.model.VehicleMake;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VehicleMakeRepository extends MongoRepository<VehicleMake, String> {
}
