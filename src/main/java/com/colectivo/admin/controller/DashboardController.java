package com.colectivo.admin.controller;

import com.colectivo.admin.dto.DashboardSnapshotDto;
import com.colectivo.admin.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardSnapshotDto> getSnapshot() {
        return ResponseEntity.ok(dashboardService.getSnapshot());
    }

    @GetMapping("/search")
    public ResponseEntity<List<DashboardSnapshotDto.SearchResultDto>> search(@RequestParam String query) {
        return ResponseEntity.ok(dashboardService.search(query));
    }
}
