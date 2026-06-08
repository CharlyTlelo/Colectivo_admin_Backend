package com.colectivo.admin.repository;

import com.colectivo.admin.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Acceso a la colección compartida {@code notifications}. El Admin solo
 * escribe (genera notificaciones para usuarios); la lectura/marcado la sirve
 * el backend de Carpool.
 */
public interface NotificationRepository extends MongoRepository<Notification, String> {
}
