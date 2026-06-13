package com.colectivo.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppUsageUserDto {
    private String userId;
    private String name;
    private String phone;
    private String email;
    private String activeProfile;
    private String clientSurface;
    private String clientSurfaceUpdatedAt;
    private String pwaInstalledAt;
}
