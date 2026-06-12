package com.colectivo.admin.repository;

import com.colectivo.admin.model.PromoBannerConfig;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PromoBannerConfigRepository extends MongoRepository<PromoBannerConfig, String> {
}
