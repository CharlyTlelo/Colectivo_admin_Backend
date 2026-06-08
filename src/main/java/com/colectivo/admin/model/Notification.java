package com.colectivo.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Notificación in-app del usuario.
 *
 * Mapea la MISMA colección {@code notifications} que usa el motor de
 * notificaciones del backend de Carpool (mx.colectivo.api.domain.Notification).
 * Ambos servicios comparten la base {@code colectivo}, por lo que un documento
 * escrito aquí aparece en la bandeja del usuario que sirve Carpool.
 *
 * El esquema debe mantenerse alineado con el de Carpool:
 *  - {@code type} se persiste como String con el nombre del enum
 *    {@code NotificationType} (p.ej. "VERIFICATION") para que Carpool lo
 *    deserialice de vuelta a su enum sin fricción.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;

    private String userId;
    private String type;
    private String title;
    private String body;
    private Map<String, String> data;
    private boolean read;
    private Instant readAt;
    private Instant createdAt;
}
