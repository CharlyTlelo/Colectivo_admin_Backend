package com.colectivo.admin.config;

import com.colectivo.admin.model.AdminUser;
import com.colectivo.admin.repository.AdminUserRepository;
import com.colectivo.admin.repository.DriverVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${colectivo.data-seeder.enabled:true}")
    private boolean enabled;

    // Credenciales del admin sembrado. NUNCA hardcodear contraseñas en el código:
    // se inyectan por entorno (Secret Manager / env vars). Si no se proveen, el
    // seed de admin se omite (no se crea un admin con contraseña por defecto).
    @Value("${colectivo.seed.admin-email:}")
    private String seedAdminEmail;

    @Value("${colectivo.seed.admin-phone:}")
    private String seedAdminPhone;

    @Value("${colectivo.seed.admin-name:Administrador}")
    private String seedAdminName;

    @Value("${colectivo.seed.admin-password:}")
    private String seedAdminPassword;

    @Override
    public void run(String... args) {
        if (!enabled) {
            log.info("Data seeder disabled, skipping startup seed.");
            return;
        }

        seedAdmin();
        seedDrivers();
    }

    private void seedAdmin() {
        if (seedAdminEmail == null || seedAdminEmail.isBlank()
                || seedAdminPassword == null || seedAdminPassword.isBlank()) {
            log.info("Admin seed skipped: colectivo.seed.admin-email/password not provided.");
            return;
        }
        if (adminUserRepository.findByEmail(seedAdminEmail).isPresent()) {
            log.info("Admin user already seeded, skipping.");
            return;
        }

        AdminUser admin = AdminUser.builder()
                .phone(seedAdminPhone != null && !seedAdminPhone.isBlank() ? seedAdminPhone : null)
                .email(seedAdminEmail)
                .name(seedAdminName)
                .passwordHash(passwordEncoder.encode(seedAdminPassword))
                .active(true)
                .createdAt(Instant.now())
                .build();

        adminUserRepository.save(admin);
        log.info("Seeded admin user: {}", seedAdminEmail);
    }

    private void seedDrivers() {
        // Disabled: drivers collection is shared with the colectivo API — no mock data here.
        log.info("seedDrivers() skipped — using shared colectivo DB.");
    }
}
