package com.colectivo.admin.dto;

import lombok.Data;

@Data
public class TripRouteRequest {
    private String localityOriginId;
    private String localityDestinationId;
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
    private boolean approved;
    private boolean active;
}
