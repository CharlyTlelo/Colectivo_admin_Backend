package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "countries")
public class Country {
    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String code;

    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
