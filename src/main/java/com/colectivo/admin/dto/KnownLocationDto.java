package com.colectivo.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class KnownLocationDto {

    private String id;
    private double lat;
    private double lng;
    private String municipalityName;
    private String localityName;
    private String label;
    private String source;
    private int confirmations;
    private String createdByUserId;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
}
