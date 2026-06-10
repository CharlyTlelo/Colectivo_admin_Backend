package com.colectivo.admin.service;

import com.colectivo.admin.dto.PaymentsSnapshotDto;
import com.colectivo.admin.model.Fine;
import com.colectivo.admin.model.User;
import com.colectivo.admin.repository.FineRepository;
import com.colectivo.admin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Monitoreo de pagos: lee las colecciones compartidas `fines` y `users`
 * (escritas por el backend de Carpool vía Mercado Pago) y arma la vista de
 * deudores y movimientos para el panel. Solo lectura — el cobro y el
 * desbloqueo (debtAmount) los automatiza el webhook de Carpool.
 */
@Service
@RequiredArgsConstructor
public class PaymentsService {

    private static final Map<String, String> REASON_LABELS = Map.of(
            "late_cancel", "Cancelación tardía",
            "no_show", "No-show",
            "driver_cancel_with_passengers", "Cancelación con pasajeros",
            "late_start_driver", "Viaje no iniciado a tiempo (conductor)",
            "late_start_passenger", "Viaje programado no realizado"
    );

    private final FineRepository fineRepository;
    private final UserRepository userRepository;

    public PaymentsSnapshotDto getSnapshot() {
        List<Fine> fines = fineRepository.findAll();

        Set<String> userIds = fines.stream()
                .map(Fine::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<String, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<Fine> pending = fines.stream().filter(f -> "pending".equals(f.getStatus())).toList();
        List<Fine> paid = fines.stream().filter(f -> "paid".equals(f.getStatus())).toList();

        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        double paidLast7 = paid.stream()
                .filter(f -> f.getPaidAt() != null && f.getPaidAt().isAfter(sevenDaysAgo))
                .mapToDouble(Fine::getAmount).sum();

        Map<String, List<Fine>> pendingByUser = pending.stream()
                .filter(f -> f.getUserId() != null)
                .collect(Collectors.groupingBy(Fine::getUserId));

        List<PaymentsSnapshotDto.DebtorDto> debtors = pendingByUser.entrySet().stream()
                .map(e -> {
                    User u = users.get(e.getKey());
                    double debt = u != null ? u.getDebtAmount()
                            : e.getValue().stream().mapToDouble(Fine::getAmount).sum();
                    Instant oldest = e.getValue().stream()
                            .map(Fine::getCreatedAt)
                            .filter(t -> t != null)
                            .min(Comparator.naturalOrder())
                            .orElse(null);
                    return new PaymentsSnapshotDto.DebtorDto(
                            e.getKey(),
                            u != null && u.getName() != null ? u.getName() : "Usuario " + shortId(e.getKey()),
                            u != null ? u.getPhone() : null,
                            debt,
                            e.getValue().size(),
                            oldest);
                })
                .sorted(Comparator.comparingDouble(PaymentsSnapshotDto.DebtorDto::debtAmount).reversed())
                .toList();

        List<PaymentsSnapshotDto.MovementDto> movements = fines.stream()
                .sorted(Comparator.comparing(Fine::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(f -> {
                    User u = users.get(f.getUserId());
                    return new PaymentsSnapshotDto.MovementDto(
                            f.getId(),
                            f.getUserId(),
                            u != null && u.getName() != null ? u.getName() : "Usuario " + shortId(f.getUserId()),
                            u != null ? u.getPhone() : null,
                            f.getAmount(),
                            f.getReason(),
                            REASON_LABELS.getOrDefault(f.getReason(), f.getReason()),
                            f.getStatus(),
                            f.getReference(),
                            f.getMpPaymentId(),
                            f.getCreatedAt(),
                            f.getPaidAt());
                })
                .toList();

        PaymentsSnapshotDto.KpisDto kpis = new PaymentsSnapshotDto.KpisDto(
                pending.stream().mapToDouble(Fine::getAmount).sum(),
                pending.size(),
                paid.stream().mapToDouble(Fine::getAmount).sum(),
                paid.size(),
                paidLast7,
                debtors.size());

        return new PaymentsSnapshotDto(Instant.now(), kpis, debtors, movements);
    }

    private static String shortId(String id) {
        if (id == null) return "?";
        return id.length() > 6 ? id.substring(id.length() - 6) : id;
    }
}
