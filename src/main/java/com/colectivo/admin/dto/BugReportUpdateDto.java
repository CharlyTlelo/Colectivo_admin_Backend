package com.colectivo.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BugReportUpdateDto {
    /** "in_review" | "resolved" | "dismissed" | "open". */
    @NotBlank
    private String status;

    @Size(max = 500)
    private String reviewNote;
}
