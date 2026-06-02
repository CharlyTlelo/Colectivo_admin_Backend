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
    private String tripId;
    private double amount;
    private String reason;          // service_fee | no_show | cancellation
    private String status;          // pending | paid | waived
    private String reference;
    private Instant createdAt;
}
