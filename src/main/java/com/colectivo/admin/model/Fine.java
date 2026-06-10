package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fines")
public class Fine {

    @Id
    private String id;

    private String userId;
    private String bookingId;
    private String tripId;
    private double amount;
    // late_cancel | no_show | driver_cancel_with_passengers | late_start_driver | late_start_passenger
    private String reason;
    private String status;          // pending | paid | waived
    private String reference;
    private Instant createdAt;
    private Instant paidAt;
    // Mercado Pago (escritos por el backend de Carpool — Checkout Pro + webhook)
    private String mpPreferenceId;
    private String mpPaymentId;
}
