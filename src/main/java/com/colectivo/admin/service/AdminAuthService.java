package com.colectivo.admin.service;

import com.colectivo.admin.dto.AuthResponseDto;
import com.colectivo.admin.dto.LoginDto;
import com.colectivo.admin.dto.OtpRequestDto;
import com.colectivo.admin.dto.OtpVerifyDto;
import com.colectivo.admin.model.AdminUser;
import com.colectivo.admin.repository.AdminUserRepository;
import com.colectivo.admin.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${colectivo.admin.otp-ttl-minutes:5}")
    private int otpTtlMinutes;

    @Value("${colectivo.admin.otp-max-attempts:3}")
    private int otpMaxAttempts;

    @Value("${colectivo.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    public AuthResponseDto login(LoginDto dto) {
        AdminUser admin = adminUserRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        if (!admin.isActive()) {
            throw new IllegalStateException("Cuenta inactiva");
        }

        if (admin.getPasswordHash() == null || !passwordEncoder.matches(dto.getPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("Credenciales incorrectas");
        }

        admin.setLastLoginAt(Instant.now());
        adminUserRepository.save(admin);

        String token = jwtService.generateToken(admin.getEmail(), admin.getName());
        return AuthResponseDto.builder()
                .token(token)
                .adminName(admin.getName())
                .phone(admin.getPhone())
                .expiresIn(jwtExpirationMs / 1000)
                .build();
    }

    public Map<String, String> requestOtp(OtpRequestDto dto) {
        AdminUser admin = adminUserRepository.findByPhone(dto.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("Phone not authorized for admin access"));

        if (!admin.isActive()) {
            throw new IllegalStateException("Admin account is inactive");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(1000000));

        admin.setOtpCode(otp);
        admin.setOtpExpiresAt(Instant.now().plus(otpTtlMinutes, ChronoUnit.MINUTES));
        admin.setOtpAttempts(0);
        adminUserRepository.save(admin);

        // In production: send via SMS provider
        log.info("OTP for {} is: {} (dev mode — not sent via SMS)", dto.getPhone(), otp);

        return Map.of(
            "message", "OTP sent successfully",
            "hint", "Check server logs for OTP in development mode"
        );
    }

    public AuthResponseDto verifyOtp(OtpVerifyDto dto) {
        AdminUser admin = adminUserRepository.findByPhone(dto.getPhone())
                .orElseThrow(() -> new IllegalArgumentException("Phone not registered"));

        if (admin.getOtpCode() == null) {
            throw new IllegalStateException("No OTP was requested. Please request one first.");
        }

        if (Instant.now().isAfter(admin.getOtpExpiresAt())) {
            throw new IllegalStateException("OTP has expired. Please request a new one.");
        }

        if (admin.getOtpAttempts() >= otpMaxAttempts) {
            throw new IllegalStateException("Too many failed attempts. Please request a new OTP.");
        }

        if (!admin.getOtpCode().equals(dto.getCode())) {
            admin.setOtpAttempts(admin.getOtpAttempts() + 1);
            adminUserRepository.save(admin);
            int remaining = otpMaxAttempts - admin.getOtpAttempts();
            throw new IllegalArgumentException("Invalid OTP. " + remaining + " attempt(s) remaining.");
        }

        // Clear OTP after successful verification
        admin.setOtpCode(null);
        admin.setOtpExpiresAt(null);
        admin.setOtpAttempts(0);
        admin.setLastLoginAt(Instant.now());
        adminUserRepository.save(admin);

        String token = jwtService.generateToken(admin.getPhone(), admin.getName());

        return AuthResponseDto.builder()
                .token(token)
                .adminName(admin.getName())
                .phone(admin.getPhone())
                .expiresIn(jwtExpirationMs / 1000)
                .build();
    }
}
