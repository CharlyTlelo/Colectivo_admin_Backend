package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "municipalities")
@CompoundIndex(name = "municipalities_state_name_unique", def = "{'stateId': 1, 'name': 1}", unique = true)
public class Municipality {
    @Id
    private String id;

    private String stateId;
    private String name;
    private MunicipalityType type;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
