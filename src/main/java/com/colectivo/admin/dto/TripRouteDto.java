package com.colectivo.admin.dto;

import com.colectivo.admin.model.TripRoute;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripRouteDto {
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
    private String createdAt;
    private String updatedAt;

    public static TripRouteDto from(TripRoute route) {
        return TripRouteDto.builder()
                .id(route.getId())
                .originArea(route.getOriginArea())
                .areaType(route.getAreaType())
                .areaKey(route.getAreaKey())
                .areaName(route.getAreaName())
                .routeDestination(route.getRouteDestination())
                .finalDestination(route.getFinalDestination())
                .meetingPointLabel(route.getMeetingPointLabel())
                .meetingPointLat(route.getMeetingPointLat())
                .meetingPointLng(route.getMeetingPointLng())
                .destinationLat(route.getDestinationLat())
                .destinationLng(route.getDestinationLng())
                .routeKey(route.getRouteKey())
                .approved(route.isApproved())
                .active(route.isActive())
                .createdAt(route.getCreatedAt() != null ? route.getCreatedAt().toString() : null)
                .updatedAt(route.getUpdatedAt() != null ? route.getUpdatedAt().toString() : null)
                .build();
    }
}
