package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
public class Booking {

    @Id
    private String id;

    private String tripId;
    private String passengerId;
    private int seats;
    private String status;      // confirmed | cancelled | no_show
    private boolean fineApplied;
    private Instant createdAt;
    private TripSnapshot tripSnapshot;
}
