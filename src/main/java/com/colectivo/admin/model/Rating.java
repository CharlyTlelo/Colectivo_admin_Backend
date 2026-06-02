package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ratings")
public class Rating {

    @Id
    private String id;

    private String tripId;
    private String raterId;
    private String rateeId;
    private int score;
    private List<String> tags;
    private String comment;
    private Instant createdAt;
}
