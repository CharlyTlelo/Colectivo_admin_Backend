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
public class QueueStatsDto {
    private long pending;
    private long overdue24h;
    private long approvedToday;
    private long rejectedToday;
    private List<DriverVerificationDto> queue;
}
