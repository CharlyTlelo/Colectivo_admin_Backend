package com.colectivo.admin.repository;

import com.colectivo.admin.model.Fine;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FineRepository extends MongoRepository<Fine, String> {

    List<Fine> findByUserId(String userId);

    List<Fine> findByStatus(String status);
}
