package com.colectivo.admin.dto;

import java.time.Instant;
import java.util.List;

/**
 * Vista de pagos/multas para el panel admin: quién debe, quién ya pagó y los
 * movimientos de Mercado Pago. Los datos viven en las colecciones compartidas
 * `fines` y `users` (las escribe el backend de Carpool; aquí solo se leen).
 */
public record PaymentsSnapshotDto(
        Instant generatedAt,
        KpisDto kpis,
        List<DebtorDto> debtors,
        List<MovementDto> movements
) {
    public record KpisDto(
            double pendingAmount,
            long pendingCount,
            double paidAmount,
            long paidCount,
            double paidAmountLast7Days,
            long debtorCount
    ) {}

    /** Usuario con adeudo activo (debtAmount > 0): está bloqueado para reservar/publicar. */
    public record DebtorDto(
            String userId,
            String name,
            String phone,
            double debtAmount,
            long pendingFines,
            Instant oldestPendingAt
    ) {}

    /** Una multa con su estado de cobro (movimiento). */
    public record MovementDto(
            String fineId,
            String userId,
            String userName,
            String userPhone,
            double amount,
            String reason,
            String reasonLabel,
            String status,
            String reference,
            String mpPaymentId,
            Instant createdAt,
            Instant paidAt
    ) {}
}
