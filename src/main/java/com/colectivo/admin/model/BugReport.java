package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Reporte de falla enviado desde la app Carpool. Esta colección la ESCRIBE
 * Carpool ({@code mx.colectivo.api.domain.BugReport}); Admin solo LEE y
 * actualiza los campos de triaje (status, reviewedBy/At, reviewNote).
 *
 * <p>Si el shape cambia en Carpool hay que reflejarlo aquí o usar {@code @Field}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bug_reports")
public class BugReport {

    @Id
    private String id;

    @Indexed
    private String userId;

    /** Perfil activo al momento del reporte ("pax" / "drv"). */
    private String userProfile;

    /** Categoría predefinida; ver el catálogo en Carpool BugReportService. */
    @Indexed
    private String category;

    private String description;

    private String userAgent;
    private String appPath;
    private String appVersion;

    /** "open" | "in_review" | "resolved" | "dismissed". */
    @Indexed
    private String status;

    private String reviewedBy;
    private Instant reviewedAt;
    private String reviewNote;

    private Instant createdAt;
}
