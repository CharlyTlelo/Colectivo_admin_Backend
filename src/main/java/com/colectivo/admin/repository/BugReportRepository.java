package com.colectivo.admin.repository;

import com.colectivo.admin.model.BugReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BugReportRepository extends MongoRepository<BugReport, String> {
    List<BugReport> findAllByOrderByCreatedAtDesc();
    List<BugReport> findByStatusOrderByCreatedAtDesc(String status);
}
