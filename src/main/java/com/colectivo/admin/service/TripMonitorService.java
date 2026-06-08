package com.colectivo.admin.service;

import com.colectivo.admin.config.ArchiveProperties;
import com.colectivo.admin.dto.TripArchiveDto;
import com.colectivo.admin.dto.TripDetailDto;
import com.colectivo.admin.model.Booking;
import com.colectivo.admin.model.Trip;
import com.colectivo.admin.model.TripArchiveIndex;
import com.colectivo.admin.model.TripSnapshot;
import com.colectivo.admin.model.User;
import com.colectivo.admin.repository.BookingRepository;
import com.colectivo.admin.repository.TripArchiveIndexRepository;
import com.colectivo.admin.repository.TripRepository;
import com.colectivo.admin.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripMonitorService {

    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TripArchiveIndexRepository archiveIndexRepository;
    private final ArchiveProperties archiveProperties;
    private final ObjectMapper objectMapper;

    public TripDetailDto getLiveTrip(String tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Viaje no encontrado"));
        return toDetail(trip);
    }

    public TripArchiveDto.ListResponse listArchives() {
        List<TripArchiveDto.ListItem> items = archiveIndexRepository.findTop100ByOrderByArchivedAtDesc()
                .stream()
                .map(this::toArchiveListItem)
                .toList();
        return new TripArchiveDto.ListResponse(items);
    }

    public TripArchiveDto.Detail getArchive(String tripId) {
        TripArchiveIndex index = archiveIndexRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Historial no encontrado"));
        Path root = Path.of(archiveProperties.getDirectory()).toAbsolutePath().normalize();
        try {
            Path jsonPath = root.resolve(index.getJsonRelativePath());
            Path txtPath = root.resolve(index.getTxtRelativePath());
            if (!Files.exists(jsonPath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo JSON no disponible");
            }
            JsonNode document = objectMapper.readTree(jsonPath.toFile());
            String textSummary = Files.exists(txtPath) ? Files.readString(txtPath) : "";
            return new TripArchiveDto.Detail(toArchiveListItem(index), document, textSummary);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo leer el historial");
        }
    }

    private TripDetailDto toDetail(Trip trip) {
        List<Booking> bookings = bookingRepository.findByTripId(trip.getId());
        Map<String, User> users = userRepository.findAll().stream()
                .filter(u -> u.getId() != null)
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        User driver = users.get(trip.getDriverId());
        List<TripDetailDto.BookingDetailDto> bookingRows = bookings.stream()
                .map(b -> {
                    User passenger = users.get(b.getPassengerId());
                    return new TripDetailDto.BookingDetailDto(
                            b.getId(),
                            b.getPassengerId(),
                            passenger != null ? passenger.getName() : null,
                            b.getSeats(),
                            b.getStatus(),
                            b.isFineApplied(),
                            b.getCreatedAt(),
                            snapshotDto(b.getTripSnapshot())
                    );
                })
                .toList();

        return new TripDetailDto(
                trip.getId(),
                trip.getStatus(),
                trip.getDriverId(),
                driver != null ? driver.getName() : null,
                trip.getDepartureTime(),
                trip.getOrigin(),
                trip.getDestination(),
                trip.getMeetingPointLabel(),
                trip.getMeetingPointDescription(),
                trip.getDestinationDetail(),
                trip.getNotes(),
                trip.getPricePerSeat(),
                trip.getCapacity(),
                trip.getTakenSeats(),
                trip.getRouteDistanceKm(),
                trip.getRouteMonitorSummary(),
                bookingRows
        );
    }

    private TripDetailDto.TripSnapshotDto snapshotDto(TripSnapshot snapshot) {
        if (snapshot == null) return null;
        return new TripDetailDto.TripSnapshotDto(
                snapshot.getOrigin(),
                snapshot.getDestination(),
                snapshot.getDepartureTime(),
                snapshot.getPricePerSeat(),
                snapshot.getMeetingPointDescription(),
                snapshot.getDestinationDetail(),
                snapshot.getNotes()
        );
    }

    private TripArchiveDto.ListItem toArchiveListItem(TripArchiveIndex index) {
        return new TripArchiveDto.ListItem(
                index.getId(),
                index.getArchivedAt(),
                index.getTerminalStatus(),
                index.getOrigin(),
                index.getDestination(),
                index.getDepartureTime(),
                index.getTakenSeats(),
                index.getCapacity(),
                index.getBookingCount(),
                index.getDriverId()
        );
    }
}
