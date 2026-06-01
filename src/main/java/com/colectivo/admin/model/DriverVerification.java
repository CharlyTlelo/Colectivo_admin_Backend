package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "drivers")
public class DriverVerification {

    @Id
    private String id;

    // User info
    private String name;

    @Indexed(unique = true)
    private String phone;

    private String email;

    // Vehicle info (from catalog)
    private String marca;
    private String modelo;
    private int anio;
    private String plate;
    private int capacity; // 4, 5, or 6

    // Document photo URLs (base64 or private URL)
    private String licenseFrontUrl;
    private String licenseBackUrl;
    private String platePhotoUrl;
    private String vehiclePhotoUrl;

    // Verification state
    private VerificationStatus verificationStatus;
    private String verificationNote;
    private String verificationDecidedBy;
    private Instant verificationDecidedAt;
    private Instant verificationRequestedAt;

    // For re-submissions: fields that were previously rejected
    private List<String> rejectedFields;
    private boolean resubmit;
    private List<String> prevRejectedFields;

    public enum VerificationStatus {
        PENDING, APPROVED, REJECTED
    }
}
