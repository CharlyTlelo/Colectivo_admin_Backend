package com.colectivo.admin.service;

import com.colectivo.admin.model.Notification;
import com.colectivo.admin.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Emite notificaciones in-app hacia usuarios desde el panel de Admin.
 *
 * Escribe directamente en la colección compartida {@code notifications}, la
 * misma que consume el motor de notificaciones de Carpool. Esto evita acoplar
 * un endpoint HTTP entre servicios reutilizando la base {@code colectivo} que
 * ambos ya comparten.
 *
 * Todas las llamadas son tolerantes a fallos: una notificación nunca debe
 * romper el flujo de negocio (aprobar/rechazar verificación).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    /** Debe coincidir con un valor de mx.colectivo.api.domain.NotificationType. */
    private static final String TYPE_VERIFICATION = "VERIFICATION";
    private static final String TYPE_SUPPORT = "SUPPORT";

    private final NotificationRepository notificationRepository;

    /** Notifica al conductor que su verificación fue aprobada. */
    public void notifyVerificationApproved(String userId, String driverId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("screen", "/driver/status");
        if (driverId != null) data.put("driverId", driverId);
        data.put("status", "approved");
        push(userId, TYPE_VERIFICATION,
                "Verificacion aprobada",
                "Tu perfil de conductor fue aprobado. Ya puedes publicar viajes.",
                data);
    }

    /** Notifica al conductor que su verificación fue rechazada (con campos y nota). */
    public void notifyVerificationRejected(String userId, String driverId,
                                           List<String> rejectedFields, String note) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("screen", "/driver/status");
        if (driverId != null) data.put("driverId", driverId);
        data.put("status", "rejected");
        if (rejectedFields != null && !rejectedFields.isEmpty()) {
            data.put("rejectedFields", String.join(",", rejectedFields));
        }
        push(userId, TYPE_VERIFICATION,
                "Verificacion rechazada",
                rejectionBody(rejectedFields, note),
                data);
    }

    public void notifySupportReply(String userId, String conversationId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("screen", "/support");
        data.put("conversationId", conversationId);
        push(userId, TYPE_SUPPORT,
                "Respuesta de soporte",
                "El equipo de soporte respondio tu conversacion.",
                data);
    }

    public void notifySupportClosed(String userId, String conversationId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("screen", "/support");
        data.put("conversationId", conversationId);
        data.put("status", "closed");
        push(userId, TYPE_SUPPORT,
                "Conversacion cerrada",
                "La conversacion fue cerrada. Si necesitas algo mas, puedes iniciar una nueva.",
                data);
    }

    public void notifySupportCloseRequest(String userId, String conversationId) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("screen", "/support");
        data.put("conversationId", conversationId);
        data.put("closeRequested", "true");
        push(userId, TYPE_SUPPORT,
                "¿Terminar conversación?",
                "El equipo de soporte pregunta si deseas cerrar esta conversación. Responde en el chat.",
                data);
    }

    private String rejectionBody(List<String> rejectedFields, String note) {
        if (note != null && !note.isBlank()) {
            return "Tu verificacion fue rechazada: " + note.trim();
        }
        if (rejectedFields != null && !rejectedFields.isEmpty()) {
            return "Tu verificacion fue rechazada. Corrige y vuelve a enviar tus documentos.";
        }
        return "Tu verificacion fue rechazada. Revisa tus documentos y vuelve a enviarlos.";
    }

    private void push(String userId, String type, String title, String body, Map<String, String> data) {
        if (userId == null || userId.isBlank()) {
            log.warn("No se pudo notificar ({}): userId vacio", type);
            return;
        }
        try {
            Notification n = Notification.builder()
                    .userId(userId)
                    .type(type)
                    .title(title)
                    .body(body)
                    .data(data)
                    .read(false)
                    .createdAt(Instant.now())
                    .build();
            notificationRepository.save(n);
        } catch (Exception e) {
            log.warn("No se pudo escribir notificacion {} para {}: {}", type, userId, e.getMessage());
        }
    }
}
