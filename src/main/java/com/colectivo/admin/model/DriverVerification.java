package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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

    // FK to users collection
    private String userId;

    // Vehicle info
    private String plate;
    private int capacity;
    private String clabe;

    // Vehicle catalog fields (set by mobile app on registration)
    private String marca;
    private String modelo;
    private int anio;

    // Document photo URLs
    private String licenseFrontUrl;
    private String licenseBackUrl;
    private String platePhotoUrl;
    private String vehiclePhotoUrl;

    // License and verification state
    private String licenseStatus;
    private VerificationStatus verificationStatus;
    private double totalEarnings;

    // Admin verification fields
    private String verificationNote;
    private String verificationDecidedBy;
    private Instant verificationDecidedAt;
    private Instant verificationRequestedAt;
    private List<String> rejectedFields;
    private boolean resubmit;
    private List<String> prevRejectedFields;

    public enum VerificationStatus {
        pending, approved, rejected
    }
}
