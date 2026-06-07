package com.colectivo.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverVerificationDto {
    private String id;
    private String userId;
    private String name;
    private String phone;
    private String email;
    private String initials;
    private String accountRole;
    private String registeredAs;
    private List<String> profiles;
    private String activeProfile;
    private String passengerProfileStatus;
    private String driverProfileStatus;
    private double rating;
    private int tripCount;
    private double debtAmount;
    private boolean blocked;
    private String userCreatedAt;
    private String marca;
    private String modelo;
    private int anio;
    private String plate;
    private int capacity;
    private String requestedAt;  // ISO string
    private double hoursAgo;
    private boolean resubmit;
    private List<String> prevRejected;
    private String verificationStatus;
    private String licenseFrontUrl;
    private String licenseBackUrl;
    private String platePhotoUrl;
    private String vehiclePhotoUrl;
    private String vehicleInteriorUrl;
    private String verificationNote;
    private String verificationDecidedBy;
    private String verificationDecidedAt;
    private List<String> rejectedFields;
}
