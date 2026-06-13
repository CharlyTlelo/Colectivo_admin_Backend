package com.colectivo.admin.controller;

import com.colectivo.admin.dto.KnownLocationDto;
import com.colectivo.admin.service.KnownLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/known-locations")
@RequiredArgsConstructor
public class KnownLocationController {

    private final KnownLocationService knownLocationService;

    /** Lista los puntos de encuentro resueltos por conductores (mapa colaborativo). */
    @GetMapping
    public ResponseEntity<List<KnownLocationDto>> list() {
        return ResponseEntity.ok(knownLocationService.list());
    }
}
