package com.colectivo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OtpVerifyDto {
    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "Code is required")
    private String code;
}
