package com.colectivo.admin.dto;

import java.util.List;

public record AppUsageSnapshotDto(
        long pwaCount,
        long browserCount,
        long unknownCount,
        List<AppUsageUserDto> users
) {}
