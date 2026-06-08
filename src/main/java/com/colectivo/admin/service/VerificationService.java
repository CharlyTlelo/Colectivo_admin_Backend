package com.colectivo.admin.service;

import com.colectivo.admin.dto.ApproveRejectDto;
import com.colectivo.admin.dto.DocumentPhotoDto;
import com.colectivo.admin.dto.DriverVerificationDto;
import com.colectivo.admin.dto.QueueStatsDto;
import com.colectivo.admin.model.DriverVerification;
import com.colectivo.admin.model.User;
import com.colectivo.admin.repository.DriverVerificationRepository;
import com.colectivo.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private static final List<String> REQUIRED_DOCUMENT_FIELDS = List.of(
            "platePhoto",
            "vehiclePhoto",
            "licFront",
            "licBack"
    );

    private static final Map<String, String> DOCUMENT_LABELS = Map.of(
            "platePhoto", "Foto de placa",
            "vehiclePhoto", "Foto del vehiculo",
            "licFront", "Licencia - frente",
            "licBack", "Licencia - reverso"
    );

    private final DriverVerificationRepository repository;
    private final UserRepository userRepository;

    public QueueStatsDto getQueue() {
        List<User> users = userRepository.findAll();
        List<String> userIds = users.stream()
                .map(User::getId)
                .filter(id -> id != null && !id.isBlank())
                .toList();
        Map<String, DriverVerification> driversByUserId = loadDriversForUsers(userIds);

        Instant now = Instant.now();
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        Instant threshold24h = now.minus(24, ChronoUnit.HOURS);

        List<DriverVerification> pending = driversByUserId.values().stream()
                .filter(driver -> driver.getVerificationStatus() == DriverVerification.VerificationStatus.pending)
                .toList();

        long overdue = pending.stream()
                .filter(d -> d.getVerificationRequestedAt() != null
                        && d.getVerificationRequestedAt().isBefore(threshold24h))
                .count();

        long approvedToday = repository.countByVerificationStatusAndVerificationDecidedAtBetween(
                DriverVerification.VerificationStatus.approved, startOfDay, now);

        long rejectedToday = repository.countByVerificationStatusAndVerificationDecidedAtBetween(
                DriverVerification.VerificationStatus.rejected, startOfDay, now);

        List<DriverVerificationDto> dtos = users.stream()
                .map(user -> toDto(driversByUserId.get(user.getId()), user))
                .toList();

        return QueueStatsDto.builder()
                .pending(pending.size())
                .overdue24h(overdue)
                .approvedToday(approvedToday)
                .rejectedToday(rejectedToday)
                .queue(dtos)
                .build();
    }

    public DriverVerificationDto getById(String id) {
        if (id.startsWith("user:")) {
            String userId = id.substring("user:".length());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            DriverVerification driver = repository.findFirstByUserIdOrderByVerificationRequestedAtDesc(userId)
                    .orElse(null);
            return toDto(driver, user);
        }

        DriverVerification driver = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));
        User user = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null)
                : null;
        return toDto(driver, user);
    }

    public DriverVerificationDto approve(String id) {
        DriverVerification driver = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));

        String decidedBy = getCurrentAdminPhone();
        Instant now = Instant.now();

        Map<String, DriverVerification.DocumentStatus> documentStatuses = allRequiredDocumentsApproved();
        driver.setDocumentStatuses(documentStatuses);
        driver.setVerificationStatus(computeVerificationStatus(documentStatuses, List.of()));
        driver.setLicenseStatus("active");
        driver.setVerificationDecidedBy(decidedBy);
        driver.setVerificationDecidedAt(now);
        driver.setRejectedFields(List.of());
        driver.setVerificationNote(null);
        repository.save(driver);

        log.info("Driver {} approved by {}", id, decidedBy);
        User user = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null)
                : null;
        return toDto(driver, user);
    }

    public DriverVerificationDto reject(String id, ApproveRejectDto dto) {
        DriverVerification driver = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));

        if (dto.getRejectedFields() == null || dto.getRejectedFields().isEmpty()) {
            throw new IllegalArgumentException("At least one rejected field is required");
        }

        String decidedBy = getCurrentAdminPhone();
        Instant now = Instant.now();
        List<String> approvedFields = dto.getApprovedFields() != null ? dto.getApprovedFields() : List.of();
        List<String> rejectedFields = dto.getRejectedFields();
        Map<String, DriverVerification.DocumentStatus> documentStatuses = statusesFromReview(approvedFields, rejectedFields);

        driver.setDocumentStatuses(documentStatuses);
        driver.setVerificationStatus(computeVerificationStatus(documentStatuses, rejectedFields));
        driver.setVerificationDecidedBy(decidedBy);
        driver.setVerificationDecidedAt(now);
        driver.setRejectedFields(rejectedFields);
        driver.setVerificationNote(dto.getNote());
        repository.save(driver);

        log.info("Driver {} rejected by {} for fields: {}", id, decidedBy, dto.getRejectedFields());
        User user = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null)
                : null;
        return toDto(driver, user);
    }

    public DriverVerificationDto suspendUser(String id) {
        if (id.startsWith("user:")) {
            User user = userRepository.findById(id.substring("user:".length()))
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
            user.setBlocked(true);
            User saved = userRepository.save(user);
            DriverVerification driver = repository.findFirstByUserIdOrderByVerificationRequestedAtDesc(saved.getId())
                    .orElse(null);
            log.info("User {} suspended by {}", saved.getId(), getCurrentAdminPhone());
            return toDto(driver, saved);
        }

        DriverVerification driver = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));

        User user = requireUserForDriver(driver);
        user.setBlocked(true);
        User saved = userRepository.save(user);

        log.info("User {} suspended by {}", saved.getId(), getCurrentAdminPhone());
        return toDto(driver, saved);
    }

    public void deleteUser(String id) {
        if (id.startsWith("user:")) {
            String userId = id.substring("user:".length());
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            repository.deleteByUserId(userId);
            userRepository.delete(user);
            log.info("User {} and driver profiles deleted by {}", user.getId(), getCurrentAdminPhone());
            return;
        }

        DriverVerification driver = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));

        User user = requireUserForDriver(driver);
        repository.delete(driver);
        repository.deleteByUserId(user.getId());
        userRepository.delete(user);

        log.info("User {} and driver profiles deleted by {}", user.getId(), getCurrentAdminPhone());
    }

    private Map<String, User> loadUsersForDrivers(List<DriverVerification> drivers) {
        List<String> userIds = drivers.stream()
                .map(DriverVerification::getUserId)
                .filter(uid -> uid != null)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
    }

    private Map<String, DriverVerification> loadDriversForUsers(List<String> userIds) {
        if (userIds.isEmpty()) return Map.of();
        return repository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        DriverVerification::getUserId,
                        driver -> driver,
                        this::chooseDriverForAdminRow
                ));
    }

    private DriverVerification chooseDriverForAdminRow(DriverVerification first, DriverVerification second) {
        if (first.getVerificationStatus() == DriverVerification.VerificationStatus.pending) return first;
        if (second.getVerificationStatus() == DriverVerification.VerificationStatus.pending) return second;
        Instant firstRequestedAt = first.getVerificationRequestedAt();
        Instant secondRequestedAt = second.getVerificationRequestedAt();
        if (firstRequestedAt == null) return second;
        if (secondRequestedAt == null) return first;
        return firstRequestedAt.isAfter(secondRequestedAt) ? first : second;
    }

    private User requireUserForDriver(DriverVerification driver) {
        if (driver.getUserId() == null || driver.getUserId().isBlank()) {
            throw new IllegalArgumentException("Driver has no linked user");
        }
        return userRepository.findById(driver.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Linked user not found: " + driver.getUserId()));
    }

    private String getCurrentAdminPhone() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal != null ? principal.toString() : "unknown";
    }

    private DriverVerificationDto toDto(DriverVerification d, User user) {
        Instant now = Instant.now();
        double hoursAgo = d != null && d.getVerificationRequestedAt() != null
                ? (double) ChronoUnit.MINUTES.between(d.getVerificationRequestedAt(), now) / 60.0
                : 0;

        String name = user != null ? user.getName() : null;
        String phone = user != null ? user.getPhone() : null;
        String email = user != null ? user.getEmail() : null;
        String initials = user != null ? user.getInitials() : computeInitials(name);
        String registeredAs = registeredAs(user);
        List<String> profiles = profiles(user, d);
        String activeProfile = activeProfile(user, profiles);
        String accountRole = activeProfile;
        String passengerProfileStatus = passengerProfileStatus(user);
        String driverProfileStatus = driverProfileStatus(d);

        Map<String, DriverVerification.DocumentStatus> documentStatuses = d != null
                ? normalizeDocumentStatuses(d)
                : Map.of();

        return DriverVerificationDto.builder()
                .id(d != null ? d.getId() : "user:" + user.getId())
                .userId(user != null ? user.getId() : d.getUserId())
                .name(name)
                .phone(phone)
                .email(email)
                .initials(initials)
                .accountRole(accountRole)
                .registeredAs(registeredAs)
                .profiles(profiles)
                .activeProfile(activeProfile)
                .passengerProfileStatus(passengerProfileStatus)
                .driverProfileStatus(driverProfileStatus)
                .rating(user != null ? user.getRating() : 0)
                .tripCount(user != null ? user.getTripCount() : 0)
                .debtAmount(user != null ? user.getDebtAmount() : 0)
                .blocked(user != null && user.isBlocked())
                .userCreatedAt(user != null && user.getCreatedAt() != null
                        ? user.getCreatedAt().toString()
                        : null)
                .marca(d != null ? d.getMarca() : null)
                .modelo(d != null ? d.getModelo() : null)
                .anio(d != null ? d.getAnio() : 0)
                .plate(d != null ? d.getPlate() : null)
                .capacity(d != null ? d.getCapacity() : 0)
                .requestedAt(d != null && d.getVerificationRequestedAt() != null
                        ? d.getVerificationRequestedAt().toString() : null)
                .hoursAgo(hoursAgo)
                .resubmit(d != null && d.isResubmit())
                .prevRejected(d != null ? d.getPrevRejectedFields() : List.of())
                .verificationStatus(d != null && d.getVerificationStatus() != null
                        ? d.getVerificationStatus().name() : null)
                .licenseFrontUrl(d != null ? d.getLicenseFrontUrl() : null)
                .licenseBackUrl(d != null ? d.getLicenseBackUrl() : null)
                .platePhotoUrl(d != null ? d.getPlatePhotoUrl() : null)
                .vehiclePhotoUrl(d != null ? d.getVehiclePhotoUrl() : null)
                .verificationNote(d != null ? d.getVerificationNote() : null)
                .verificationDecidedBy(d != null ? d.getVerificationDecidedBy() : null)
                .verificationDecidedAt(d != null && d.getVerificationDecidedAt() != null
                        ? d.getVerificationDecidedAt().toString() : null)
                .rejectedFields(d != null ? d.getRejectedFields() : List.of())
                .documentStatuses(stringifyStatuses(documentStatuses))
                .approvedPhotos(d != null ? approvedPhotos(d, documentStatuses) : List.of())
                .build();
    }

    private List<DocumentPhotoDto> approvedPhotos(
            DriverVerification driver,
            Map<String, DriverVerification.DocumentStatus> documentStatuses
    ) {
        return REQUIRED_DOCUMENT_FIELDS.stream()
                .filter(field -> documentStatuses.get(field) == DriverVerification.DocumentStatus.approved)
                .map(field -> DocumentPhotoDto.builder()
                        .field(field)
                        .label(DOCUMENT_LABELS.getOrDefault(field, field))
                        .url(documentUrl(driver, field))
                        .build())
                .filter(photo -> photo.getUrl() != null && !photo.getUrl().isBlank())
                .toList();
    }

    private String documentUrl(DriverVerification driver, String field) {
        return switch (field) {
            case "platePhoto" -> driver.getPlatePhotoUrl();
            case "vehiclePhoto" -> driver.getVehiclePhotoUrl();
            case "licFront" -> driver.getLicenseFrontUrl();
            case "licBack" -> driver.getLicenseBackUrl();
            default -> null;
        };
    }

    private Map<String, DriverVerification.DocumentStatus> allRequiredDocumentsApproved() {
        Map<String, DriverVerification.DocumentStatus> statuses = new LinkedHashMap<>();
        REQUIRED_DOCUMENT_FIELDS.forEach(field -> statuses.put(field, DriverVerification.DocumentStatus.approved));
        return statuses;
    }

    private Map<String, DriverVerification.DocumentStatus> statusesFromReview(
            List<String> approvedFields,
            List<String> rejectedFields
    ) {
        Set<String> approved = approvedFields != null ? new LinkedHashSet<>(approvedFields) : Set.of();
        Set<String> rejected = rejectedFields != null ? new LinkedHashSet<>(rejectedFields) : Set.of();
        Map<String, DriverVerification.DocumentStatus> statuses = new LinkedHashMap<>();

        for (String field : REQUIRED_DOCUMENT_FIELDS) {
            if (rejected.contains(field)) {
                statuses.put(field, DriverVerification.DocumentStatus.rejected);
            } else if (approved.contains(field)) {
                statuses.put(field, DriverVerification.DocumentStatus.approved);
            } else {
                statuses.put(field, DriverVerification.DocumentStatus.pending);
            }
        }

        return statuses;
    }

    private Map<String, DriverVerification.DocumentStatus> normalizeDocumentStatuses(DriverVerification driver) {
        Map<String, DriverVerification.DocumentStatus> saved = driver.getDocumentStatuses();
        List<String> rejectedFields = driver.getRejectedFields() != null ? driver.getRejectedFields() : List.of();
        Map<String, DriverVerification.DocumentStatus> statuses = new LinkedHashMap<>();

        for (String field : REQUIRED_DOCUMENT_FIELDS) {
            DriverVerification.DocumentStatus status = saved != null ? saved.get(field) : null;
            if (status == null) {
                status = inferLegacyDocumentStatus(driver, rejectedFields, field);
            }
            statuses.put(field, status);
        }

        return statuses;
    }

    private DriverVerification.DocumentStatus inferLegacyDocumentStatus(
            DriverVerification driver,
            List<String> rejectedFields,
            String field
    ) {
        if (rejectedFields.contains(field)) {
            return DriverVerification.DocumentStatus.rejected;
        }
        if (driver.getVerificationStatus() == DriverVerification.VerificationStatus.approved) {
            return DriverVerification.DocumentStatus.approved;
        }
        return DriverVerification.DocumentStatus.pending;
    }

    private Map<String, String> stringifyStatuses(Map<String, DriverVerification.DocumentStatus> statuses) {
        Map<String, String> result = new LinkedHashMap<>();
        statuses.forEach((field, status) -> result.put(field, status.name()));
        return result;
    }

    private DriverVerification.VerificationStatus computeVerificationStatus(
            Map<String, DriverVerification.DocumentStatus> documentStatuses,
            List<String> rejectedFields
    ) {
        if (rejectedFields != null && !rejectedFields.isEmpty()) {
            return DriverVerification.VerificationStatus.rejected;
        }
        if (documentStatuses.values().stream().anyMatch(status -> status == DriverVerification.DocumentStatus.rejected)) {
            return DriverVerification.VerificationStatus.rejected;
        }
        boolean allApproved = REQUIRED_DOCUMENT_FIELDS.stream()
                .allMatch(field -> documentStatuses.get(field) == DriverVerification.DocumentStatus.approved);
        return allApproved
                ? DriverVerification.VerificationStatus.approved
                : DriverVerification.VerificationStatus.pending;
    }

    private String registeredAs(User user) {
        if (user == null) return null;
        String registeredAs = normalizeProfile(user.getRegisteredAs());
        if (registeredAs != null) return registeredAs;
        return normalizeProfile(user.getRole());
    }

    private List<String> profiles(User user, DriverVerification driver) {
        Set<String> normalizedProfiles = new LinkedHashSet<>();
        if (user != null && user.getProfiles() != null) {
            user.getProfiles().stream()
                    .map(this::normalizeProfile)
                    .filter(profile -> profile != null)
                    .forEach(normalizedProfiles::add);
        }

        String registeredAs = registeredAs(user);
        if (registeredAs != null) {
            normalizedProfiles.add(registeredAs);
        }

        if (driver != null) {
            normalizedProfiles.add("drv");
        }

        return new ArrayList<>(normalizedProfiles);
    }

    private String activeProfile(User user, List<String> profiles) {
        String activeProfile = user != null ? normalizeProfile(user.getActiveProfile()) : null;
        if (activeProfile != null && profiles.contains(activeProfile)) {
            return activeProfile;
        }

        String roleProfile = user != null ? normalizeProfile(user.getRole()) : null;
        if (roleProfile != null && profiles.contains(roleProfile)) {
            return roleProfile;
        }

        return profiles.isEmpty() ? null : profiles.get(0);
    }

    private String normalizeProfile(String profile) {
        if (profile == null || profile.isBlank()) return null;
        return switch (profile.trim().toLowerCase()) {
            case "pax", "passenger", "pasajero" -> "pax";
            case "drv", "driver", "conductor" -> "drv";
            default -> profile.trim().toLowerCase();
        };
    }

    private String passengerProfileStatus(User user) {
        if (user == null) return "missing";
        return user.getName() != null && !user.getName().isBlank()
                ? "registered"
                : "incomplete";
    }

    private String driverProfileStatus(DriverVerification driver) {
        if (driver == null || driver.getVerificationStatus() == null) return "incomplete";
        return driver.getVerificationStatus().name();
    }

    private String computeInitials(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
