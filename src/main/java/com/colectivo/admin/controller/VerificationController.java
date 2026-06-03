package com.colectivo.admin.controller;

import com.colectivo.admin.dto.ApproveRejectDto;
import com.colectivo.admin.dto.DriverVerificationDto;
import com.colectivo.admin.dto.QueueStatsDto;
import com.colectivo.admin.service.VerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/verifications")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping
    public ResponseEntity<QueueStatsDto> getQueue() {
        return ResponseEntity.ok(verificationService.getQueue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DriverVerificationDto> getById(@PathVariable String id) {
        return ResponseEntity.ok(verificationService.getById(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<DriverVerificationDto> approve(@PathVariable String id) {
        return ResponseEntity.ok(verificationService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DriverVerificationDto> reject(
            @PathVariable String id,
            @RequestBody ApproveRejectDto dto) {
        return ResponseEntity.ok(verificationService.reject(id, dto));
    }

    @PostMapping("/{id}/user/suspend")
    public ResponseEntity<DriverVerificationDto> suspendUser(@PathVariable String id) {
        return ResponseEntity.ok(verificationService.suspendUser(id));
    }

    @DeleteMapping("/{id}/user")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        verificationService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
