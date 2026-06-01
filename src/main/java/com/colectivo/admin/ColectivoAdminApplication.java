package com.colectivo.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ColectivoAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(ColectivoAdminApplication.class, args);
        System.out.println("Colectivo Admin Application esta corriendo");
    }
}
