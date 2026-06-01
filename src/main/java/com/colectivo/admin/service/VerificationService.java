package com.colectivo.admin.service;

import com.colectivo.admin.dto.ApproveRejectDto;
import com.colectivo.admin.dto.DriverVerificationDto;
import com.colectivo.admin.dto.QueueStatsDto;
import com.colectivo.admin.model.DriverVerification;
import com.colectivo.admin.repository.DriverVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final DriverVerificationRepository repository;

    public QueueStatsDto getQueue() {
        List<DriverVerification> pending = repository
                .findByVerificationStatusOrderByVerificationRequestedAtAsc(
                        DriverVerification.VerificationStatus.PENDING);

        Instant now = Instant.now();
        Instant startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        Instant threshold24h = now.minus(24, ChronoUnit.HOURS);

        long overdue = pending.stream()
                .filter(d -> d.getVerificationRequestedAt() != null
                        && d.getVerificationRequestedAt().isBefore(threshold24h))
                .count();

        long approvedToday = repository.countByVerificationStatusAndVerificationDecidedAtBetween(
                DriverVerification.VerificationStatus.APPROVED, startOfDay, now);

        long rejectedToday = repository.countByVerificationStatusAndVerificationDecidedAtBetween(
                DriverVerification.VerificationStatus.REJECTED, startOfDay, now);

        List<DriverVerificationDto> dtos = pending.stream()
                .map(this::toDto)
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
        return toDto(driver);
    }

    public DriverVerificationDto approve(String id) {
        DriverVerification driver = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));

        String decidedBy = getCurrentAdminPhone();
        Instant now = Instant.now();

        driver.setVerificationStatus(DriverVerification.VerificationStatus.APPROVED);
        driver.setVerificationDecidedBy(decidedBy);
        driver.setVerificationDecidedAt(now);
        driver.setRejectedFields(List.of());
        driver.setVerificationNote(null);
        repository.save(driver);

        log.info("Driver {} approved by {}", id, decidedBy);
        return toDto(driver);
    }

    public DriverVerificationDto reject(String id, ApproveRejectDto dto) {
        DriverVerification driver = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + id));

        if (dto.getRejectedFields() == null || dto.getRejectedFields().isEmpty()) {
            throw new IllegalArgumentException("At least one rejected field is required");
        }

        String decidedBy = getCurrentAdminPhone();
        Instant now = Instant.now();

        driver.setVerificationStatus(DriverVerification.VerificationStatus.REJECTED);
        driver.setVerificationDecidedBy(decidedBy);
        driver.setVerificationDecidedAt(now);
        driver.setRejectedFields(dto.getRejectedFields());
        driver.setVerificationNote(dto.getNote());
        repository.save(driver);

        log.info("Driver {} rejected by {} for fields: {}", id, decidedBy, dto.getRejectedFields());
        return toDto(driver);
    }

    private String getCurrentAdminPhone() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal != null ? principal.toString() : "unknown";
    }

    private DriverVerificationDto toDto(DriverVerification d) {
        Instant now = Instant.now();
        double hoursAgo = d.getVerificationRequestedAt() != null
                ? (double) ChronoUnit.MINUTES.between(d.getVerificationRequestedAt(), now) / 60.0
                : 0;

        String requestedAtStr = d.getVerificationRequestedAt() != null
                ? d.getVerificationRequestedAt().toString()
                : null;

        String decidedAtStr = d.getVerificationDecidedAt() != null
                ? d.getVerificationDecidedAt().toString()
                : null;

        String initials = computeInitials(d.getName());

        return DriverVerificationDto.builder()
                .id(d.getId())
                .name(d.getName())
                .phone(d.getPhone())
                .email(d.getEmail())
                .initials(initials)
                .marca(d.getMarca())
                .modelo(d.getModelo())
                .anio(d.getAnio())
                .plate(d.getPlate())
                .capacity(d.getCapacity())
                .requestedAt(requestedAtStr)
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
                .verificationDecidedAt(decidedAtStr)
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
