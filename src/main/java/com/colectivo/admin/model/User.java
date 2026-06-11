package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String phone;

    private String name;
    private String email;
    private String initials;
    private String role;        // "pax" | "driver"
    private String registeredAs; // "pax" | "drv"
    private List<String> profiles;
    private String activeProfile; // "pax" | "drv"
    private double rating;
    private int tripCount;
    private double debtAmount;
    private boolean blocked;
    private Integer remainingOpportunities;
    private Instant createdAt;
}
