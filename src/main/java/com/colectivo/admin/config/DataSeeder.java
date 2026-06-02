package com.colectivo.admin.config;

import com.colectivo.admin.model.AdminUser;
import com.colectivo.admin.repository.AdminUserRepository;
import com.colectivo.admin.repository.DriverVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;


@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final DriverVerificationRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdmin();
        seedDrivers();
    }

    private void seedAdmin() {
        String adminEmail = "carlos_tlelo@hotmail.com";
        if (adminUserRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already seeded, skipping.");
            return;
        }

        AdminUser admin = AdminUser.builder()
                .phone("+525512345678")
                .email(adminEmail)
                .name("Carlos Tlelo · Dirección")
                .passwordHash(passwordEncoder.encode("MasDcp4ne7"))
                .active(true)
                .createdAt(Instant.now())
                .build();

        adminUserRepository.save(admin);
        log.info("Seeded admin user: {}", adminEmail);
    }

    private void seedDrivers() {
        // Disabled: drivers collection is shared with the colectivo API — no mock data here.
        log.info("seedDrivers() skipped — using shared colectivo DB.");
    }
}
