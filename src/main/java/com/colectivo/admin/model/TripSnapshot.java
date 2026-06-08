package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSnapshot {

    private String localityOriginId;
    private String localityDestinationId;
    private String origin;
    private String destination;
    private Instant departureTime;
    private double pricePerSeat;
    private int capacity;
    private String meetingPointLabel;
    private String meetingPointDescription;
    private String destinationDetail;
    private String notes;
}
