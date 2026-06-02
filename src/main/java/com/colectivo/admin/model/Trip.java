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
@Document(collection = "trips")
public class Trip {

    @Id
    private String id;

    private String driverId;
    private String origin;
    private String destination;
    private Instant departureTime;
    private double pricePerSeat;
    private int capacity;
    private int takenSeats;
    private String status;          // published | full | in_progress | completed | cancelled
    private double meetingPointLat;
    private double meetingPointLng;
    private String meetingPointLabel;
    private Instant createdAt;
}
