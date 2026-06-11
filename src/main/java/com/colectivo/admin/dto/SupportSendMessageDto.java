package com.colectivo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupportSendMessageDto {
    @NotBlank
    private String content;
}
