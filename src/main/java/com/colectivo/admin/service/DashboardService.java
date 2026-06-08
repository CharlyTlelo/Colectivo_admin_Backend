package com.colectivo.admin.service;

import com.colectivo.admin.dto.DashboardSnapshotDto;
import com.colectivo.admin.model.Booking;
import com.colectivo.admin.model.DriverVerification;
import com.colectivo.admin.model.Fine;
import com.colectivo.admin.model.Rating;
import com.colectivo.admin.model.Trip;
import com.colectivo.admin.model.User;
import com.colectivo.admin.repository.BookingRepository;
import com.colectivo.admin.repository.DriverVerificationRepository;
import com.colectivo.admin.repository.FineRepository;
import com.colectivo.admin.repository.RatingRepository;
import com.colectivo.admin.repository.TripRepository;
import com.colectivo.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final ZoneId ADMIN_ZONE = ZoneId.of("America/Mexico_City");
    private static final double DEBT_ALERT_THRESHOLD = 500.0;
    private static final int SEARCH_LIMIT = 20;

    private final UserRepository userRepository;
    private final DriverVerificationRepository driverRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;
    private final FineRepository fineRepository;
    private final RatingRepository ratingRepository;

    public DashboardSnapshotDto getSnapshot() {
        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now(ADMIN_ZONE).atStartOfDay(ADMIN_ZONE).toInstant();
        Instant sevenDaysAgo = now.minus(7, ChronoUnit.DAYS);
        Instant twentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS);

        List<User> users = userRepository.findAll();
        List<DriverVerification> drivers = driverRepository.findAll();
        List<Trip> trips = tripRepository.findAll();
        List<Booking> bookings = bookingRepository.findAll();
        List<Fine> fines = fineRepository.findAll();
        List<Rating> ratings = ratingRepository.findAll();

        List<DriverVerification> pendingDrivers = drivers.stream()
                .filter(driver -> driver.getVerificationStatus() == DriverVerification.VerificationStatus.pending)
                .toList();
        long overdueVerifications = pendingDrivers.stream()
                .filter(driver -> driver.getVerificationRequestedAt() != null
                        && driver.getVerificationRequestedAt().isBefore(twentyFourHoursAgo))
                .count();
        long approvedDrivers = drivers.stream()
                .filter(driver -> driver.getVerificationStatus() == DriverVerification.VerificationStatus.approved)
                .count();
        long blockedUsers = users.stream().filter(User::isBlocked).count();
        long usersWithDebt = users.stream().filter(user -> user.getDebtAmount() > 0).count();
        double totalDebt = users.stream().mapToDouble(User::getDebtAmount).sum();
        long activeTrips = trips.stream().filter(this::isActiveTrip).count();
        long tripsPublishedToday = trips.stream()
                .filter(trip -> hasStatus(trip, "published"))
                .filter(trip -> isToday(firstInstant(trip.getDepartureTime(), trip.getCreatedAt()), startOfToday, now))
                .count();
        long bookingsToday = bookings.stream()
                .filter(booking -> isToday(booking.getCreatedAt(), startOfToday, now))
                .count();
        long pendingFines = fines.stream().filter(fine -> hasStatus(fine, "pending")).count();
        long lowRatings = ratings.stream().filter(rating -> rating.getScore() > 0 && rating.getScore() <= 2).count();

        List<DashboardSnapshotDto.KpiDto> kpis = List.of(
                kpi("pendingVerifications", "Conductores pendientes", pendingDrivers.size(), overdueVerifications + " con mas de 24 h", toneFor(overdueVerifications, "danger", "amber")),
                kpi("activeTrips", "Viajes activos ahora", activeTrips, "boarding o en curso", activeTrips > 0 ? "blue" : "neutral"),
                kpi("publishedToday", "Viajes publicados hoy", tripsPublishedToday, "salidas o altas del dia", "green"),
                kpi("bookingsToday", "Reservas de hoy", bookingsToday, "confirmadas, canceladas o no-show", "blue"),
                kpi("pendingFines", "Multas pendientes", pendingFines, "usuarios por cobrar", pendingFines > 0 ? "amber" : "neutral"),
                kpi("totalDebt", "Deuda total", money(totalDebt), usersWithDebt + " usuarios con deuda", totalDebt > 0 ? "danger" : "neutral"),
                kpi("blockedUsers", "Usuarios bloqueados", blockedUsers, "bloqueo operativo activo", blockedUsers > 0 ? "danger" : "neutral"),
                kpi("approvedDrivers", "Conductores aprobados", approvedDrivers, "perfiles listos para operar", "green")
        );

        List<DashboardSnapshotDto.AlertDto> alerts = buildAlerts(
                pendingDrivers,
                drivers,
                trips,
                bookings,
                fines,
                users,
                ratings,
                now,
                twentyFourHoursAgo,
                sevenDaysAgo
        );

        List<DashboardSnapshotDto.SectionDto> sections = List.of(
                section("verifications", "Verificaciones pendientes", "Ordenadas por antiguedad y riesgo", pendingVerificationItems(pendingDrivers, users, now)),
                section("activeTrips", "Viajes activos", "Estados que requieren seguimiento operativo", activeTripItems(trips, now)),
                section("incidents", "Incidentes operativos", "Reservas, multas y no-shows recientes", incidentItems(bookings, fines, users, sevenDaysAgo)),
                section("quality", "Calidad y calificaciones", "Ratings bajos y usuarios con promedio delicado", qualityItems(ratings, users, sevenDaysAgo))
        );

        return new DashboardSnapshotDto(now, kpis, alerts, sections);
    }

    public List<DashboardSnapshotDto.SearchResultDto> search(String query) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.length() < 2) {
            return List.of();
        }

        List<DashboardSnapshotDto.SearchResultDto> results = new ArrayList<>();
        searchUsers(normalizedQuery, results);
        searchDrivers(normalizedQuery, results);
        searchTrips(normalizedQuery, results);
        searchBookings(normalizedQuery, results);
        searchFines(normalizedQuery, results);
        return results.stream().limit(SEARCH_LIMIT).toList();
    }

    private void searchUsers(String query, List<DashboardSnapshotDto.SearchResultDto> results) {
        userRepository.findAll().stream()
                .filter(user -> matches(query, user.getId(), user.getName(), user.getPhone(), user.getEmail()))
                .limit(SEARCH_LIMIT)
                .map(user -> new DashboardSnapshotDto.SearchResultDto(
                        "Usuario",
                        user.getId(),
                        fallback(user.getName(), "Usuario sin nombre"),
                        maskContact(user.getPhone(), user.getEmail()) + " - deuda " + money(user.getDebtAmount()),
                        user.isBlocked() ? "Bloqueado" : "Activo",
                        user.isBlocked() || user.getDebtAmount() > 0 ? "danger" : "green",
                        "/verifications"
                ))
                .forEach(results::add);
    }

    private void searchDrivers(String query, List<DashboardSnapshotDto.SearchResultDto> results) {
        driverRepository.findAll().stream()
                .filter(driver -> matches(query, driver.getId(), driver.getUserId(), driver.getPlate(), driver.getMarca(), driver.getModelo()))
                .limit(SEARCH_LIMIT)
                .map(driver -> new DashboardSnapshotDto.SearchResultDto(
                        "Conductor",
                        driver.getId(),
                        fallback(driver.getPlate(), "Sin placa"),
                        vehicleLabel(driver),
                        driver.getVerificationStatus() != null ? driver.getVerificationStatus().name() : "Sin estado",
                        driver.getVerificationStatus() == DriverVerification.VerificationStatus.pending ? "amber" : "green",
                        "/verifications/" + driver.getId() + "/review"
                ))
                .forEach(results::add);
    }

    private void searchTrips(String query, List<DashboardSnapshotDto.SearchResultDto> results) {
        tripRepository.findAll().stream()
                .filter(trip -> matches(query, trip.getId(), trip.getDriverId(), trip.getOrigin(), trip.getDestination()))
                .limit(SEARCH_LIMIT)
                .map(trip -> new DashboardSnapshotDto.SearchResultDto(
                        "Viaje",
                        trip.getId(),
                        fallback(trip.getOrigin(), "Origen sin dato") + " -> " + fallback(trip.getDestination(), "Destino sin dato"),
                        "Salida " + dateLabel(trip.getDepartureTime()) + " - " + trip.getTakenSeats() + "/" + trip.getCapacity() + " asientos",
                        fallback(trip.getStatus(), "Sin estado"),
                        toneForTrip(trip),
                        "/dashboard"
                ))
                .forEach(results::add);
    }

    private void searchBookings(String query, List<DashboardSnapshotDto.SearchResultDto> results) {
        bookingRepository.findAll().stream()
                .filter(booking -> matches(query, booking.getId(), booking.getTripId(), booking.getPassengerId()))
                .limit(SEARCH_LIMIT)
                .map(booking -> new DashboardSnapshotDto.SearchResultDto(
                        "Reserva",
                        booking.getId(),
                        "Reserva " + shortId(booking.getId()),
                        booking.getSeats() + " asiento(s) - viaje " + shortId(booking.getTripId()),
                        fallback(booking.getStatus(), "Sin estado"),
                        hasStatus(booking, "no_show", "cancelled") ? "danger" : "green",
                        "/dashboard"
                ))
                .forEach(results::add);
    }

    private void searchFines(String query, List<DashboardSnapshotDto.SearchResultDto> results) {
        fineRepository.findAll().stream()
                .filter(fine -> matches(query, fine.getId(), fine.getReference(), fine.getUserId(), fine.getTripId(), fine.getReason()))
                .limit(SEARCH_LIMIT)
                .map(fine -> new DashboardSnapshotDto.SearchResultDto(
                        "Multa",
                        fine.getId(),
                        fallback(fine.getReference(), "Multa " + shortId(fine.getId())),
                        money(fine.getAmount()) + " - " + fallback(fine.getReason(), "sin motivo"),
                        fallback(fine.getStatus(), "Sin estado"),
                        hasStatus(fine, "pending") ? "amber" : "green",
                        "/dashboard"
                ))
                .forEach(results::add);
    }

    private List<DashboardSnapshotDto.AlertDto> buildAlerts(
            List<DriverVerification> pendingDrivers,
            List<DriverVerification> allDrivers,
            List<Trip> trips,
            List<Booking> bookings,
            List<Fine> fines,
            List<User> users,
            List<Rating> ratings,
            Instant now,
            Instant twentyFourHoursAgo,
            Instant sevenDaysAgo
    ) {
        List<DashboardSnapshotDto.AlertDto> alerts = new ArrayList<>();
        pendingDrivers.stream()
                .filter(driver -> driver.getVerificationRequestedAt() != null
                        && driver.getVerificationRequestedAt().isBefore(twentyFourHoursAgo))
                .forEach(driver -> alerts.add(alert(
                        "critical",
                        "Verificacion pendiente por mas de 24 h",
                        vehicleLabel(driver) + " lleva " + hoursBetween(driver.getVerificationRequestedAt(), now) + " h en cola",
                        "driver",
                        driver.getId(),
                        "/verifications/" + driver.getId() + "/review"
                )));

        pendingDrivers.stream()
                .filter(this::hasMissingDriverDocs)
                .forEach(driver -> alerts.add(alert(
                        "warning",
                        "Documentos incompletos",
                        vehicleLabel(driver) + " requiere revisar licencia, placa o foto del vehiculo",
                        "driver",
                        driver.getId(),
                        "/verifications/" + driver.getId() + "/review"
                )));

        duplicatePlates(allDrivers).forEach((plate, count) -> alerts.add(alert(
                "critical",
                "Placa repetida",
                plate.toUpperCase(Locale.ROOT) + " aparece en " + count + " perfiles de conductor",
                "driver",
                plate,
                "/verifications"
        )));

        trips.stream()
                .filter(trip -> trip.getCapacity() > 0 && trip.getTakenSeats() > trip.getCapacity())
                .forEach(trip -> alerts.add(alert(
                        "critical",
                        "Viaje con sobrecupo",
                        shortId(trip.getId()) + " tiene " + trip.getTakenSeats() + "/" + trip.getCapacity() + " asientos",
                        "trip",
                        trip.getId(),
                        "/dashboard"
                )));

        trips.stream()
                .filter(trip -> hasStatus(trip, "published") && trip.getDepartureTime() != null && trip.getDepartureTime().isBefore(now))
                .forEach(trip -> alerts.add(alert(
                        "warning",
                        "Viaje publicado con salida pasada",
                        fallback(trip.getOrigin(), "Origen") + " -> " + fallback(trip.getDestination(), "Destino") + " salia " + dateLabel(trip.getDepartureTime()),
                        "trip",
                        trip.getId(),
                        "/dashboard"
                )));

        trips.stream()
                .filter(trip -> hasStatus(trip, "boarding") && trip.getDepartureTime() != null && trip.getDepartureTime().isBefore(now.minus(2, ChronoUnit.HOURS)))
                .forEach(trip -> alerts.add(alert(
                        "warning",
                        "Viaje en boarding demasiado tiempo",
                        shortId(trip.getId()) + " lleva mas de 2 h sin avanzar",
                        "trip",
                        trip.getId(),
                        "/dashboard"
                )));

        trips.stream()
                .filter(trip -> hasStatus(trip, "in_progress") && trip.getDepartureTime() != null && trip.getDepartureTime().isBefore(now.minus(8, ChronoUnit.HOURS)))
                .forEach(trip -> alerts.add(alert(
                        "warning",
                        "Viaje en curso demasiado tiempo",
                        shortId(trip.getId()) + " lleva mas de 8 h en curso",
                        "trip",
                        trip.getId(),
                        "/dashboard"
                )));

        Map<String, Trip> tripsById = trips.stream()
                .filter(trip -> trip.getId() != null)
                .collect(Collectors.toMap(Trip::getId, Function.identity(), (first, second) -> first));
        bookings.stream()
                .filter(booking -> hasStatus(booking, "confirmed"))
                .filter(booking -> {
                    Trip trip = tripsById.get(booking.getTripId());
                    return trip != null && hasStatus(trip, "cancelled");
                })
                .forEach(booking -> alerts.add(alert(
                        "critical",
                        "Reserva confirmada en viaje cancelado",
                        "Reserva " + shortId(booking.getId()) + " sigue activa en viaje " + shortId(booking.getTripId()),
                        "booking",
                        booking.getId(),
                        "/dashboard"
                )));

        fines.stream()
                .filter(fine -> hasStatus(fine, "pending") && fine.getCreatedAt() != null && fine.getCreatedAt().isBefore(sevenDaysAgo))
                .forEach(fine -> alerts.add(alert(
                        "warning",
                        "Multa pendiente por mas de 7 dias",
                        fallback(fine.getReference(), shortId(fine.getId())) + " por " + money(fine.getAmount()),
                        "fine",
                        fine.getId(),
                        "/dashboard"
                )));

        users.stream()
                .filter(user -> user.getDebtAmount() > DEBT_ALERT_THRESHOLD)
                .forEach(user -> alerts.add(alert(
                        "warning",
                        "Usuario con deuda alta",
                        fallback(user.getName(), shortId(user.getId())) + " acumula " + money(user.getDebtAmount()),
                        "user",
                        user.getId(),
                        "/verifications"
                )));

        ratings.stream()
                .filter(rating -> rating.getScore() > 0 && rating.getScore() <= 2)
                .filter(rating -> rating.getCreatedAt() == null || rating.getCreatedAt().isAfter(sevenDaysAgo))
                .forEach(rating -> alerts.add(alert(
                        "warning",
                        "Rating bajo reciente",
                        "Score " + rating.getScore() + " en viaje " + shortId(rating.getTripId()),
                        "rating",
                        rating.getId(),
                        "/dashboard"
                )));

        return alerts.stream()
                .sorted(Comparator.comparingInt(this::severityOrder))
                .limit(12)
                .toList();
    }

    private List<DashboardSnapshotDto.ListItemDto> pendingVerificationItems(List<DriverVerification> pendingDrivers, List<User> users, Instant now) {
        Map<String, User> usersById = users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, second) -> first));

        return pendingDrivers.stream()
                .sorted(Comparator.comparing(driver -> nullableInstant(driver.getVerificationRequestedAt())))
                .limit(6)
                .map(driver -> {
                    User user = usersById.get(driver.getUserId());
                    long hours = hoursBetween(driver.getVerificationRequestedAt(), now);
                    return item(
                            fallback(user != null ? user.getName() : null, vehicleLabel(driver)),
                            vehicleLabel(driver) + " - " + maskContact(user != null ? user.getPhone() : null, user != null ? user.getEmail() : null),
                            hours >= 24 ? hours + " h pendiente" : "En cola",
                            hours >= 24 ? "danger" : "amber",
                            "/verifications/" + driver.getId() + "/review"
                    );
                })
                .toList();
    }

    private List<DashboardSnapshotDto.ListItemDto> activeTripItems(List<Trip> trips, Instant now) {
        return trips.stream()
                .filter(this::isActiveTrip)
                .sorted(Comparator.comparing(trip -> nullableInstant(trip.getDepartureTime())))
                .limit(6)
                .map(trip -> item(
                        fallback(trip.getOrigin(), "Origen sin dato") + " -> " + fallback(trip.getDestination(), "Destino sin dato"),
                        activeTripMeta(trip),
                        fallback(trip.getStatus(), "Activo"),
                        toneForTrip(trip),
                        "/dashboard"
                ))
                .toList();
    }

    private List<DashboardSnapshotDto.ListItemDto> incidentItems(List<Booking> bookings, List<Fine> fines, List<User> users, Instant sevenDaysAgo) {
        Map<String, User> usersById = users.stream()
                .filter(user -> user.getId() != null)
                .collect(Collectors.toMap(User::getId, Function.identity(), (first, second) -> first));

        List<DashboardSnapshotDto.ListItemDto> items = new ArrayList<>();
        bookings.stream()
                .filter(booking -> hasStatus(booking, "no_show", "cancelled"))
                .filter(booking -> booking.getCreatedAt() == null || booking.getCreatedAt().isAfter(sevenDaysAgo))
                .limit(4)
                .map(booking -> {
                    User user = usersById.get(booking.getPassengerId());
                    return item(
                            fallback(user != null ? user.getName() : null, "Pasajero " + shortId(booking.getPassengerId())),
                            fallback(booking.getStatus(), "incidente") + " - " + booking.getSeats() + " asiento(s)",
                            dateLabel(booking.getCreatedAt()),
                            hasStatus(booking, "no_show") ? "danger" : "amber",
                            "/dashboard"
                    );
                })
                .forEach(items::add);

        fines.stream()
                .filter(fine -> hasStatus(fine, "pending"))
                .sorted(Comparator.comparing(Fine::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(4)
                .map(fine -> {
                    User user = usersById.get(fine.getUserId());
                    return item(
                            fallback(fine.getReference(), "Multa " + shortId(fine.getId())),
                            fallback(user != null ? user.getName() : null, "Usuario " + shortId(fine.getUserId())) + " - " + fallback(fine.getReason(), "sin motivo"),
                            money(fine.getAmount()),
                            "amber",
                            "/dashboard"
                    );
                })
                .forEach(items::add);

        return items.stream().limit(6).toList();
    }

    private List<DashboardSnapshotDto.ListItemDto> qualityItems(List<Rating> ratings, List<User> users, Instant sevenDaysAgo) {
        List<DashboardSnapshotDto.ListItemDto> items = new ArrayList<>();
        ratings.stream()
                .filter(rating -> rating.getScore() > 0 && rating.getScore() <= 2)
                .filter(rating -> rating.getCreatedAt() == null || rating.getCreatedAt().isAfter(sevenDaysAgo))
                .sorted(Comparator.comparing(Rating::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(rating -> item(
                        "Rating " + rating.getScore() + "/5",
                        fallback(rating.getComment(), "Sin comentario") + " - viaje " + shortId(rating.getTripId()),
                        dateLabel(rating.getCreatedAt()),
                        "danger",
                        "/dashboard"
                ))
                .forEach(items::add);

        users.stream()
                .filter(user -> user.getRating() > 0 && user.getRating() < 3.5)
                .sorted(Comparator.comparingDouble(User::getRating))
                .limit(4)
                .map(user -> item(
                        fallback(user.getName(), "Usuario " + shortId(user.getId())),
                        maskContact(user.getPhone(), user.getEmail()),
                        String.format(Locale.US, "%.1f/5", user.getRating()),
                        "amber",
                        "/verifications"
                ))
                .forEach(items::add);

        return items.stream().limit(6).toList();
    }

    private DashboardSnapshotDto.KpiDto kpi(String key, String label, long value, String detail, String tone) {
        return kpi(key, label, Long.toString(value), detail, tone);
    }

    private DashboardSnapshotDto.KpiDto kpi(String key, String label, String value, String detail, String tone) {
        return new DashboardSnapshotDto.KpiDto(key, label, value, detail, tone);
    }

    private DashboardSnapshotDto.AlertDto alert(String severity, String title, String description, String entityType, String entityId, String route) {
        return new DashboardSnapshotDto.AlertDto(severity, title, description, entityType, entityId, route);
    }

    private DashboardSnapshotDto.SectionDto section(String key, String title, String description, List<DashboardSnapshotDto.ListItemDto> items) {
        return new DashboardSnapshotDto.SectionDto(key, title, description, items);
    }

    private DashboardSnapshotDto.ListItemDto item(String title, String meta, String status, String tone, String route) {
        return new DashboardSnapshotDto.ListItemDto(title, meta, status, tone, route);
    }

    private Map<String, Long> duplicatePlates(List<DriverVerification> drivers) {
        Map<String, Long> counts = drivers.stream()
                .map(DriverVerification::getPlate)
                .filter(plate -> plate != null && !plate.isBlank())
                .map(plate -> plate.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        counts.entrySet().removeIf(entry -> entry.getValue() < 2);
        return counts;
    }

    private boolean hasMissingDriverDocs(DriverVerification driver) {
        return isBlank(driver.getLicenseFrontUrl())
                || isBlank(driver.getLicenseBackUrl())
                || isBlank(driver.getPlatePhotoUrl())
                || isBlank(driver.getVehiclePhotoUrl())
                || isBlank(driver.getPlate())
                || isBlank(driver.getMarca())
                || isBlank(driver.getModelo())
                || driver.getAnio() <= 0
                || driver.getCapacity() <= 0;
    }

    private boolean isActiveTrip(Trip trip) {
        return hasStatus(trip, "boarding", "in_progress");
    }

    private boolean hasStatus(Trip trip, String... statuses) {
        return containsStatus(trip != null ? trip.getStatus() : null, statuses);
    }

    private boolean hasStatus(Booking booking, String... statuses) {
        return containsStatus(booking != null ? booking.getStatus() : null, statuses);
    }

    private boolean hasStatus(Fine fine, String... statuses) {
        return containsStatus(fine != null ? fine.getStatus() : null, statuses);
    }

    private boolean containsStatus(String status, String... statuses) {
        String normalizedStatus = normalizeStatus(status);
        for (String candidate : statuses) {
            if (normalizedStatus.equals(normalizeStatus(candidate))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeStatus(String status) {
        if (status == null) return "";
        return status.trim().toLowerCase(Locale.ROOT).replace("-", "_");
    }

    private boolean isToday(Instant value, Instant startOfToday, Instant now) {
        return value != null && !value.isBefore(startOfToday) && !value.isAfter(now);
    }

    private Instant firstInstant(Instant preferred, Instant fallback) {
        return preferred != null ? preferred : fallback;
    }

    private Instant nullableInstant(Instant instant) {
        return instant != null ? instant : Instant.EPOCH;
    }

    private long hoursBetween(Instant from, Instant to) {
        if (from == null) return 0;
        return Math.max(0, ChronoUnit.HOURS.between(from, to));
    }

    private String toneFor(long value, String positiveTone, String zeroTone) {
        return value > 0 ? positiveTone : zeroTone;
    }

    private String toneForTrip(Trip trip) {
        if ("needs_review".equals(normalizeStatus(trip.getRouteMonitorStatus()))) return "danger";
        if ("long_route".equals(normalizeStatus(trip.getRouteMonitorStatus()))) return "amber";
        if (trip.getCapacity() > 0 && trip.getTakenSeats() > trip.getCapacity()) return "danger";
        if (hasStatus(trip, "cancelled")) return "danger";
        if (hasStatus(trip, "boarding", "in_progress")) return "blue";
        if (hasStatus(trip, "published")) return "green";
        return "neutral";
    }

    private String activeTripMeta(Trip trip) {
        String base = "Salida " + dateLabel(trip.getDepartureTime()) + " - " + trip.getTakenSeats() + "/" + trip.getCapacity() + " asientos";
        if (trip.getRouteMonitorSummary() != null && !trip.getRouteMonitorSummary().isBlank()) {
            return base + " - " + trip.getRouteMonitorSummary();
        }
        String destinationDetail = fallback(trip.getDestinationDetail(), trip.getFinalDestinationDescription());
        if (destinationDetail != null && !destinationDetail.isBlank()) {
            return base + " - final: " + destinationDetail;
        }
        return base;
    }

    private int severityOrder(DashboardSnapshotDto.AlertDto alert) {
        return switch (alert.severity()) {
            case "critical" -> 0;
            case "warning" -> 1;
            default -> 2;
        };
    }

    private String vehicleLabel(DriverVerification driver) {
        String vehicle = List.of(driver.getMarca(), driver.getModelo(), driver.getPlate()).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
        return vehicle.isBlank() ? "Conductor " + shortId(driver.getId()) : vehicle;
    }

    private String dateLabel(Instant value) {
        if (value == null) return "Sin fecha";
        return value.atZone(ADMIN_ZONE).toLocalDateTime().toString().replace('T', ' ');
    }

    private String money(double value) {
        return "$" + String.format(Locale.US, "%,.2f", value);
    }

    private String shortId(String id) {
        if (id == null || id.isBlank()) return "sin-id";
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean matches(String query, String... values) {
        for (String value : values) {
            if (normalize(value).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
        return normalized;
    }

    private String maskContact(String phone, String email) {
        String maskedPhone = maskPhone(phone);
        String maskedEmail = maskEmail(email);
        if (!maskedPhone.isBlank() && !maskedEmail.isBlank()) {
            return maskedPhone + " - " + maskedEmail;
        }
        if (!maskedPhone.isBlank()) return maskedPhone;
        if (!maskedEmail.isBlank()) return maskedEmail;
        return "Sin contacto";
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) return "****";
        return "***" + digits.substring(digits.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) return "";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
