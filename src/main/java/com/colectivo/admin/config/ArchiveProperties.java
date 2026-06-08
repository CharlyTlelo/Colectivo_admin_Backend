package com.colectivo.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "colectivo.archive")
public class ArchiveProperties {
    private String directory = "./data/trip-archives";
}
