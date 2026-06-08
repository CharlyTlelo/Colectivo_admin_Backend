package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "states")
@CompoundIndexes({
        @CompoundIndex(name = "states_country_name_unique", def = "{'countryId': 1, 'name': 1}", unique = true),
        @CompoundIndex(name = "states_country_code_unique", def = "{'countryId': 1, 'code': 1}", unique = true)
})
public class GeoState {
    @Id
    private String id;

    private String countryId;
    private String name;
    private String code;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
