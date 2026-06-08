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
@Document(collection = "trip_routes")
public class TripRoute {
    @Id
    private String id;

    private String originArea;
    private String areaType;
    private String areaKey;
    private String areaName;
    private String routeDestination;
    private String finalDestination;
    private String meetingPointLabel;
    private double meetingPointLat;
    private double meetingPointLng;
    private double destinationLat;
    private double destinationLng;
    private String routeKey;
    private boolean approved;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
