package com.colectivo.admin.config;

import com.colectivo.admin.model.AdminUser;
import com.colectivo.admin.model.DriverVerification;
import com.colectivo.admin.repository.AdminUserRepository;
import com.colectivo.admin.repository.DriverVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
        if (driverRepository.count() > 0) {
            log.info("Drivers already seeded, skipping.");
            return;
        }

        Instant now = Instant.now();

        // 1. Juan Vázquez — 2h ago
        DriverVerification juan = DriverVerification.builder()
                .name("Juan Vázquez")
                .phone("+525551234511")
                .email("juan.vazquez@gmail.com")
                .marca("Nissan")
                .modelo("Versa")
                .anio(2019)
                .plate("MEX-5512")
                .capacity(5)
                .verificationStatus(DriverVerification.VerificationStatus.PENDING)
                .verificationRequestedAt(now.minus(2, ChronoUnit.HOURS))
                .resubmit(false)
                .build();

        // 2. Carmen Mejía — 5h ago
        DriverVerification carmen = DriverVerification.builder()
                .name("Carmen Mejía")
                .phone("+525577320091")
                .email(null)
                .marca("Volkswagen")
                .modelo("Vento")
                .anio(2021)
                .plate("MEX-7732")
                .capacity(4)
                .verificationStatus(DriverVerification.VerificationStatus.PENDING)
                .verificationRequestedAt(now.minus(5, ChronoUnit.HOURS))
                .resubmit(false)
                .build();

        // 3. Omar Rivera — 9h ago
        DriverVerification omar = DriverVerification.builder()
                .name("Omar Rivera")
                .phone("+525511998420")
                .email("omar.rivera@hotmail.com")
                .marca("Toyota")
                .modelo("Avanza")
                .anio(2018)
                .plate("MEX-1199")
                .capacity(6)
                .verificationStatus(DriverVerification.VerificationStatus.PENDING)
                .verificationRequestedAt(now.minus(9, ChronoUnit.HOURS))
                .resubmit(false)
                .build();

        // 4. Nadia Santos — 26h ago (>24h, highlight)
        DriverVerification nadia = DriverVerification.builder()
                .name("Nadia Santos")
                .phone("+525598471102")
                .email(null)
                .marca("Kia")
                .modelo("Rio")
                .anio(2020)
                .plate("MEX-9847")
                .capacity(5)
                .verificationStatus(DriverVerification.VerificationStatus.PENDING)
                .verificationRequestedAt(now.minus(26, ChronoUnit.HOURS))
                .resubmit(false)
                .build();

        // 5. Raúl Guzmán — 40min ago (re-submit, previously rejected licBack)
        DriverVerification raul = DriverVerification.builder()
                .name("Raúl Guzmán")
                .phone("+525533227788")
                .email("raul.guzman@gmail.com")
                .marca("Chevrolet")
                .modelo("Aveo")
                .anio(2017)
                .plate("MEX-3322")
                .capacity(4)
                .verificationStatus(DriverVerification.VerificationStatus.PENDING)
                .verificationRequestedAt(now.minus(40, ChronoUnit.MINUTES))
                .resubmit(true)
                .prevRejectedFields(List.of("licBack"))
                .build();

        List<DriverVerification> drivers = List.of(juan, carmen, omar, nadia, raul);
        driverRepository.saveAll(drivers);
        log.info("Seeded {} mock drivers", drivers.size());
    }
}
