package com.colectivo.admin.controller;

import com.colectivo.admin.dto.AuthResponseDto;
import com.colectivo.admin.dto.LoginDto;
import com.colectivo.admin.dto.OtpRequestDto;
import com.colectivo.admin.dto.OtpVerifyDto;
import com.colectivo.admin.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginDto dto) {
        return ResponseEntity.ok(adminAuthService.login(dto));
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, String>> requestOtp(@Valid @RequestBody OtpRequestDto dto) {
        Map<String, String> result = adminAuthService.requestOtp(dto);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponseDto> verifyOtp(@Valid @RequestBody OtpVerifyDto dto) {
        AuthResponseDto result = adminAuthService.verifyOtp(dto);
        return ResponseEntity.ok(result);
    }
}
