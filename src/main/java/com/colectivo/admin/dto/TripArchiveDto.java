package com.colectivo.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;

public final class TripArchiveDto {

    private TripArchiveDto() {}

    public record ListItem(
            String tripId,
            Instant archivedAt,
            String terminalStatus,
            String origin,
            String destination,
            Instant departureTime,
            int takenSeats,
            int capacity,
            int bookingCount,
            String driverId
    ) {}

    public record Detail(
            ListItem summary,
            JsonNode document,
            String textSummary
    ) {}

    public record ListResponse(
            List<ListItem> items
    ) {}
}
