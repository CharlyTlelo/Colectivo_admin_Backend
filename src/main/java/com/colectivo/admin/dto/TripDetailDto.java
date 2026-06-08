package com.colectivo.admin.dto;

import java.time.Instant;
import java.util.List;

public record TripDetailDto(
        String id,
        String status,
        String driverId,
        String driverName,
        Instant departureTime,
        String origin,
        String destination,
        String meetingPointLabel,
        String meetingPointDescription,
        String destinationDetail,
        String notes,
        double pricePerSeat,
        int capacity,
        int takenSeats,
        double routeDistanceKm,
        String routeMonitorSummary,
        List<BookingDetailDto> bookings
) {
    public record BookingDetailDto(
            String id,
            String passengerId,
            String passengerName,
            int seats,
            String status,
            boolean fineApplied,
            Instant createdAt,
            TripSnapshotDto snapshot
    ) {}

    public record TripSnapshotDto(
            String origin,
            String destination,
            Instant departureTime,
            double pricePerSeat,
            String meetingPointDescription,
            String destinationDetail,
            String notes
    ) {}
}
