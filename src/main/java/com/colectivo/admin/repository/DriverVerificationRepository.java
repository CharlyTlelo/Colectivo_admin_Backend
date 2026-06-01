package com.colectivo.admin.repository;

import com.colectivo.admin.model.DriverVerification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface DriverVerificationRepository extends MongoRepository<DriverVerification, String> {

    List<DriverVerification> findByVerificationStatusOrderByVerificationRequestedAtAsc(
            DriverVerification.VerificationStatus status);

    long countByVerificationStatus(DriverVerification.VerificationStatus status);

    long countByVerificationStatusAndVerificationDecidedAtBetween(
            DriverVerification.VerificationStatus status, Instant from, Instant to);

    long countByVerificationStatusAndVerificationRequestedAtBefore(
            DriverVerification.VerificationStatus status, Instant threshold);
}
