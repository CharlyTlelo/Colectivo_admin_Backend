package com.colectivo.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.colectivo.admin.config.ArchiveProperties;

@SpringBootApplication
@EnableConfigurationProperties(ArchiveProperties.class)
public class ColectivoAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(ColectivoAdminApplication.class, args);
        System.out.println("Colectivo Admin Application esta corriendo");
    }
}
