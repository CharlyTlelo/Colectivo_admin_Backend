package com.colectivo.admin.service;

import com.colectivo.admin.dto.ApproveRejectDto;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final DriverVerificationRepository repository;
    private final UserRepository userRepository;

    public QueueStatsDto getQueue() {
        List<DriverVerification> pending = repository
                .findByVerificationStatusOrderByVerificationRequestedAtAsc(
                        DriverVerification.VerificationStatus.pending);

        // Bulk-load users for all pending drivers
        Map<String, User> usersById = loadUsersForDrivers(pending);

        Instant now = Instant.now();
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        Instant threshold24h = now.minus(24, ChronoUnit.HOURS);

        long overdue = pending.stream()
                .filter(d -> d.getVerificationRequestedAt() != null
                        && d.getVerificationRequestedAt().isBefore(threshold24h))
                .count();

        long approvedToday = repository.countByVerificationStatusAndVerificationDecidedAtBetween(
                DriverVerification.VerificationStatus.approved, startOfDay, now);

        long rejectedToday = repository.countByVerificationStatusAndVerificationDecidedAtBetween(
                DriverVerification.VerificationStatus.rejected, startOfDay, now);

        List<DriverVerificationDto> dtos = pending.stream()
                .map(d -> toDto(d, usersById.get(d.getUserId())))
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

        driver.setVerificationStatus(DriverVerification.VerificationStatus.approved);
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

        driver.setVerificationStatus(DriverVerification.VerificationStatus.rejected);
        driver.setVerificationDecidedBy(decidedBy);
        driver.setVerificationDecidedAt(now);
        driver.setRejectedFields(dto.getRejectedFields());
        driver.setVerificationNote(dto.getNote());
        repository.save(driver);

        log.info("Driver {} rejected by {} for fields: {}", id, decidedBy, dto.getRejectedFields());
        User user = driver.getUserId() != null
                ? userRepository.findById(driver.getUserId()).orElse(null)
                : null;
        return toDto(driver, user);
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

    private String getCurrentAdminPhone() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal != null ? principal.toString() : "unknown";
    }

    private DriverVerificationDto toDto(DriverVerification d, User user) {
        Instant now = Instant.now();
        double hoursAgo = d.getVerificationRequestedAt() != null
                ? (double) ChronoUnit.MINUTES.between(d.getVerificationRequestedAt(), now) / 60.0
                : 0;

        String name = user != null ? user.getName() : null;
        String phone = user != null ? user.getPhone() : null;
        String email = user != null ? user.getEmail() : null;
        String initials = user != null ? user.getInitials() : computeInitials(name);

        return DriverVerificationDto.builder()
                .id(d.getId())
                .name(name)
                .phone(phone)
                .email(email)
                .initials(initials)
                .marca(d.getMarca())
                .modelo(d.getModelo())
                .anio(d.getAnio())
                .plate(d.getPlate())
                .capacity(d.getCapacity())
                .requestedAt(d.getVerificationRequestedAt() != null
                        ? d.getVerificationRequestedAt().toString() : null)
                .hoursAgo(hoursAgo)
                .resubmit(d.isResubmit())
                .prevRejected(d.getPrevRejectedFields())
                .verificationStatus(d.getVerificationStatus() != null
                        ? d.getVerificationStatus().name() : null)
                .licenseFrontUrl(d.getLicenseFrontUrl())
                .licenseBackUrl(d.getLicenseBackUrl())
                .platePhotoUrl(d.getPlatePhotoUrl())
                .vehiclePhotoUrl(d.getVehiclePhotoUrl())
                .verificationNote(d.getVerificationNote())
                .verificationDecidedBy(d.getVerificationDecidedBy())
                .verificationDecidedAt(d.getVerificationDecidedAt() != null
                        ? d.getVerificationDecidedAt().toString() : null)
                .rejectedFields(d.getRejectedFields())
                .build();
    }

    private String computeInitials(String name) {
        if (name == null || name.isBlank()) return "??";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
