package com.colectivo.admin.controller;

import com.colectivo.admin.dto.PaymentsSnapshotDto;
import com.colectivo.admin.service.PaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class PaymentsController {

    private final PaymentsService paymentsService;

    @GetMapping
    public ResponseEntity<PaymentsSnapshotDto> getSnapshot() {
        return ResponseEntity.ok(paymentsService.getSnapshot());
    }
}
