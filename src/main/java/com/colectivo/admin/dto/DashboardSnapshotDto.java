package com.colectivo.admin.dto;

import java.time.Instant;
import java.util.List;

public record DashboardSnapshotDto(
        Instant generatedAt,
        List<KpiDto> kpis,
        List<AlertDto> alerts,
        List<SectionDto> sections
) {
    public record KpiDto(
            String key,
            String label,
            String value,
            String detail,
            String tone
    ) {}

    public record AlertDto(
            String severity,
            String title,
            String description,
            String entityType,
            String entityId,
            String route
    ) {}

    public record SectionDto(
            String key,
            String title,
            String description,
            List<ListItemDto> items
    ) {}

    public record ListItemDto(
            String title,
            String meta,
            String status,
            String tone,
            String route
    ) {}

    public record SearchResultDto(
            String type,
            String id,
            String title,
            String subtitle,
            String status,
            String tone,
            String route
    ) {}
}
