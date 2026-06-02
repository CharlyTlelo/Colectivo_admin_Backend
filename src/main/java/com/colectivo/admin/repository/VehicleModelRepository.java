package com.colectivo.admin.repository;

import com.colectivo.admin.model.VehicleModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleModelRepository extends MongoRepository<VehicleModel, String> {

    List<VehicleModel> findByMakeId(String makeId);
}
