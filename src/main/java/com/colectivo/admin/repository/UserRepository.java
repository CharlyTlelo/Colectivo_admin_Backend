package com.colectivo.admin.repository;

import com.colectivo.admin.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByPhone(String phone);

    List<User> findByRole(String role);

    List<User> findByBlockedTrue();
}
