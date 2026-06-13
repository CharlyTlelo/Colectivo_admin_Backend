package com.colectivo.admin.controller;

import com.colectivo.admin.dto.AppUsageSnapshotDto;
import com.colectivo.admin.service.AppUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/app-usage")
@RequiredArgsConstructor
public class AppUsageController {

    private final AppUsageService appUsageService;

    /** Usuarios que usan la PWA instalada vs el navegador. */
    @GetMapping
    public ResponseEntity<AppUsageSnapshotDto> snapshot() {
        return ResponseEntity.ok(appUsageService.getSnapshot());
    }
}
