package com.colectivo.admin.config;

import com.colectivo.admin.model.Country;
import com.colectivo.admin.model.GeoState;
import com.colectivo.admin.model.Locality;
import com.colectivo.admin.model.Municipality;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
@RequiredArgsConstructor
public class GeographicCatalogIndexConfig {
    private final MongoTemplate mongoTemplate;

    @Bean
    public ApplicationRunner ensureGeographicCatalogIndexes() {
        return args -> {
            mongoTemplate.indexOps(Country.class).ensureIndex(
                    new Index().on("code", Sort.Direction.ASC).unique().named("countries_code_unique"));
            mongoTemplate.indexOps(GeoState.class).ensureIndex(
                    new Index().on("countryId", Sort.Direction.ASC).on("name", Sort.Direction.ASC).unique().named("states_country_name_unique"));
            mongoTemplate.indexOps(GeoState.class).ensureIndex(
                    new Index().on("countryId", Sort.Direction.ASC).on("code", Sort.Direction.ASC).unique().named("states_country_code_unique"));
            mongoTemplate.indexOps(Municipality.class).ensureIndex(
                    new Index().on("stateId", Sort.Direction.ASC).on("name", Sort.Direction.ASC).unique().named("municipalities_state_name_unique"));
            mongoTemplate.indexOps(Locality.class).ensureIndex(
                    new Index().on("municipalityId", Sort.Direction.ASC).on("name", Sort.Direction.ASC).unique().named("localities_municipality_name_unique"));
        };
    }
}
