package com.colectivo.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugReportDto {

    private String id;
    private String userId;
    /** Nombre legible del usuario (resolvido por el service). */
    private String userName;
    private String userPhone;
    private String userProfile;
    private String category;
    private String categoryLabel;
    private String description;
    private String userAgent;
    private String appPath;
    private String appVersion;
    private String status;
    private String reviewedBy;
    private Instant reviewedAt;
    private String reviewNote;
    private Instant createdAt;
}
