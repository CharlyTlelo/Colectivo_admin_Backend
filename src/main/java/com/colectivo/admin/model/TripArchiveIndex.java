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
@Document(collection = "trip_archive_index")
public class TripArchiveIndex {

    @Id
    private String id;

    private Instant archivedAt;
    private String terminalStatus;
    private String driverId;
    private String origin;
    private String destination;
    private Instant departureTime;
    private int takenSeats;
    private int capacity;
    private int bookingCount;
    private String jsonRelativePath;
    private String txtRelativePath;
}
