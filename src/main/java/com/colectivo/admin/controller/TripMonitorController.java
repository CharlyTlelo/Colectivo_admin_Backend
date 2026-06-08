package com.colectivo.admin.controller;

import com.colectivo.admin.dto.TripArchiveDto;
import com.colectivo.admin.dto.TripDetailDto;
import com.colectivo.admin.service.TripMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/trips")
@RequiredArgsConstructor
public class TripMonitorController {

    private final TripMonitorService tripMonitorService;

    @GetMapping("/archives")
    public ResponseEntity<TripArchiveDto.ListResponse> listArchives() {
        return ResponseEntity.ok(tripMonitorService.listArchives());
    }

    @GetMapping("/archives/{tripId}")
    public ResponseEntity<TripArchiveDto.Detail> getArchive(@PathVariable String tripId) {
        return ResponseEntity.ok(tripMonitorService.getArchive(tripId));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<TripDetailDto> getLiveTrip(@PathVariable String tripId) {
        return ResponseEntity.ok(tripMonitorService.getLiveTrip(tripId));
    }
}
