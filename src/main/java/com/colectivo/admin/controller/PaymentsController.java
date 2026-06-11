package com.colectivo.admin.controller;

import com.colectivo.admin.dto.PaymentsSnapshotDto;
import com.colectivo.admin.service.PaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class PaymentsController {

    private final PaymentsService paymentsService;

    @GetMapping
    public ResponseEntity<PaymentsSnapshotDto> getSnapshot() {
        return ResponseEntity.ok(paymentsService.getSnapshot());
    }

    @PostMapping("/users/{userId}/waive-debt")
    public ResponseEntity<Void> waiveUserDebt(@PathVariable String userId) {
        try {
            paymentsService.waiveUserDebt(userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
    }
}
