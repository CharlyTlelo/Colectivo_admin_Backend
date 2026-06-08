package com.colectivo.admin.controller;

import com.colectivo.admin.dto.TripRouteDto;
import com.colectivo.admin.dto.TripRouteRequest;
import com.colectivo.admin.service.TripRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/trip-routes")
@RequiredArgsConstructor
public class TripRouteController {
    private final TripRouteService tripRouteService;

    @GetMapping
    public ResponseEntity<List<TripRouteDto>> list() {
        return ResponseEntity.ok(tripRouteService.list());
    }

    @PostMapping
    public ResponseEntity<TripRouteDto> create(@RequestBody TripRouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tripRouteService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TripRouteDto> update(@PathVariable String id, @RequestBody TripRouteRequest request) {
        return ResponseEntity.ok(tripRouteService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        tripRouteService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
