package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admin_users")
public class AdminUser {

    @Id
    private String id;

    @Indexed(unique = true)
    private String phone;

    @Indexed(unique = true, sparse = true)
    private String email;

    private String name;
    private String passwordHash;

    // Current pending OTP
    private String otpCode;
    private Instant otpExpiresAt;
    private int otpAttempts;

    private boolean active;
    private Instant createdAt;
    private Instant lastLoginAt;
}
