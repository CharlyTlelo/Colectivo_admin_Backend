package com.colectivo.admin.service;

import com.colectivo.admin.dto.BugReportDto;
import com.colectivo.admin.dto.BugReportUpdateDto;
import com.colectivo.admin.exception.NotFoundException;
import com.colectivo.admin.model.BugReport;
import com.colectivo.admin.model.User;
import com.colectivo.admin.repository.BugReportRepository;
import com.colectivo.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lee la colección compartida {@code bug_reports} (escrita por Carpool) y
 * permite al admin marcar el estado de triaje. La creación NO ocurre aquí:
 * llega desde la app Carpool vía {@code POST /api/bug-reports}.
 */
@Service
@RequiredArgsConstructor
public class BugReportService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("open", "in_review", "resolved", "dismissed");

    /**
     * Etiquetas legibles para Admin. Mantener sincronizadas con el catálogo
     * en Carpool ({@code BugReportService.ALLOWED_CATEGORIES} y el array
     * {@code CATEGORIES} en {@code ReportBug.jsx}).
     */
    private static final Map<String, String> CATEGORY_LABELS = Map.ofEntries(
            Map.entry("publish_trip",    "Publicar viaje (conductor)"),
            Map.entry("book_trip",       "Reservar viaje (pasajero)"),
            Map.entry("chat",            "Chat del viaje"),
            Map.entry("map_location",    "Mapa o ubicación"),
            Map.entry("payment_fine",    "Pago o multa"),
            Map.entry("notifications",   "Notificaciones"),
            Map.entry("account_profile", "Cuenta o perfil"),
            Map.entry("performance",     "Rendimiento (lento / se traba)"),
            Map.entry("display",         "Visualización en el teléfono"),
            Map.entry("prefer_native_app", "Prefiero una app"),
            Map.entry("other",           "Otro")
    );

    private final BugReportRepository bugReportRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public List<BugReportDto> list(String status) {
        List<BugReport> reports = (status == null || status.isBlank())
                ? bugReportRepository.findAllByOrderByCreatedAtDesc()
                : bugReportRepository.findByStatusOrderByCreatedAtDesc(status.trim().toLowerCase());

        Map<String, User> users = lookupUsers(reports);
        return reports.stream()
                .map(report -> toDto(report, users.get(report.getUserId())))
                .collect(Collectors.toList());
    }

    public BugReportDto getById(String id) {
        BugReport report = bugReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reporte no encontrado: " + id));
        User user = userRepository.findById(report.getUserId()).orElse(null);
        return toDto(report, user);
    }

    public BugReportDto update(String id, BugReportUpdateDto dto, Authentication auth) {
        BugReport report = bugReportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Reporte no encontrado: " + id));

        String status = dto.getStatus() != null ? dto.getStatus().trim().toLowerCase() : "";
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Estado no válido: " + status);
        }

        report.setStatus(status);
        report.setReviewNote(dto.getReviewNote());
        // Solo se sella reviewedBy/At al sacar del bucket "open" (en_review/resolved/dismissed).
        if (!"open".equals(status)) {
            report.setReviewedBy(adminEmail(auth));
            report.setReviewedAt(Instant.now(clock));
        }
        report = bugReportRepository.save(report);

        User user = userRepository.findById(report.getUserId()).orElse(null);
        return toDto(report, user);
    }

    private String adminEmail(Authentication auth) {
        return auth != null && auth.getName() != null ? auth.getName() : "system";
    }

    private Map<String, User> lookupUsers(List<BugReport> reports) {
        List<String> ids = reports.stream()
                .map(BugReport::getUserId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, User> map = new HashMap<>();
        userRepository.findAllById(ids).forEach(u -> map.put(u.getId(), u));
        return map;
    }

    private BugReportDto toDto(BugReport report, User user) {
        return BugReportDto.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .userName(user != null ? user.getName() : null)
                .userPhone(user != null ? user.getPhone() : null)
                .userProfile(report.getUserProfile())
                .category(report.getCategory())
                .categoryLabel(CATEGORY_LABELS.getOrDefault(report.getCategory(), report.getCategory()))
                .description(report.getDescription())
                .userAgent(report.getUserAgent())
                .appPath(report.getAppPath())
                .appVersion(report.getAppVersion())
                .status(report.getStatus())
                .reviewedBy(report.getReviewedBy())
                .reviewedAt(report.getReviewedAt())
                .reviewNote(report.getReviewNote())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
