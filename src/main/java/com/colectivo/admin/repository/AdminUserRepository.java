package com.colectivo.admin.repository;

import com.colectivo.admin.model.AdminUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminUserRepository extends MongoRepository<AdminUser, String> {
    Optional<AdminUser> findByPhone(String phone);
    Optional<AdminUser> findByEmail(String email);
}
