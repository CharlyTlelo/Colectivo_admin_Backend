# Colectivo Admin Backend — Claude Code Configuration

## Ecosistema Colectivo

Este repo es **una de dos plataformas** que componen Colectivo. Comparten **visión, base de datos (Mongo Atlas) y GCP project (`colectivo-497901`)**, pero tienen tareas distintas:

| Plataforma | Repo | Tarea |
|---|---|---|
| **Admin** (esta) | `CharlyTlelo/Colectivo_admin_Frontend` (Angular 17) + `CharlyTlelo/Colectivo_admin_Backend` (Spring Boot, este) | Back-office: cola de verificaciones, aprobar/rechazar, suspender, dashboard. |
| **Carpool** (hermana) | `CharlyTlelo/Colectivo_frontend` (React+Vite) + `CharlyTlelo/Colectivo_backend` (Spring Boot) | Cara user-facing: conductor crea cuenta, sube documentos, ve estado, publica viajes; pasajero busca y reserva. |

**Lo importante para este backend:**
- Lee y escribe la colección `drivers` en Mongo Atlas. Carpool escribe el documento inicial + reenvíos; este admin backend escribe **sólo los campos de verificación** (`documentStatuses`, `rejectedFields`, `verificationStatus`, `verificationDecidedBy`, `verificationDecidedAt`, `verificationNote`) y bloqueo (`users.blocked`).
- El modelo `DriverVerification.java` usa `@Field` para remapear nombres que Carpool usa con otra convención: `vehicleMakeName→marca`, `vehicleModelName→modelo`, `vehicleYear→anio`. **No renombrar las propiedades Java**; el DTO y el frontend admin dependen de `marca/modelo/anio`.
- `verificationStatus` enum lowercase: `pending` | `approved` | `rejected`. El DTO serializa con `.name()` → lowercase. El frontend admin tiene `VerificationStatus` type matching esto.
- Required documents canónicos: `platePhoto`, `vehiclePhoto`, `licFront`, `licBack`. `vehicleInterior` retirado del review aunque el modelo aún tiene `vehicleInteriorUrl` (compat retro con Carpool).
- Endpoints: `GET /api/v1/admin/verifications` (cola con stats), `GET /api/v1/admin/verifications/{id}` (detalle), `POST .../approve`, `POST .../reject {approvedFields, rejectedFields, note}`, `POST .../user/suspend`, `DELETE .../user`.
- `computeVerificationStatus`: si hay `rejectedFields` o algún `documentStatuses.value === 'rejected'` → `rejected`. Si todos los required son `approved` → `approved`. Sino → `pending`.
- Push a `main` = deploy a producción automático (Cloud Build → Cloud Run `colectivo-admin-backend` us-central1).
- Cuenta GitHub: **CharlyTlelo**.

**URLs:**
- Backend: https://colectivo-admin-backend-940989231421.us-central1.run.app
- Frontend admin que lo consume: https://colectivo-admin.web.app
- Backend Carpool (separado, NO este): Cloud Run `colectivo-api` us-south1.

**Deuda conocida — REQUIRED_DOCUMENT_FIELDS duplicado:** la lista `["platePhoto", "vehiclePhoto", "licFront", "licBack"]` y `computeVerificationStatus` viven tanto aquí como en `Colectivo_backend/.../DriverService.java`. Hoy están alineados; cualquier cambio futuro exige tocar ambos repos + ambos frontends (`REVIEW_ITEMS` en admin, `DOCS` en Carpool). Pasos al agregar un doc: (1) ambos `REQUIRED_DOCUMENT_FIELDS`, (2) admin `REVIEW_ITEMS`, (3) Carpool `DOCS`, (4) E2E registro→reenvío→aprobación.

**Cuándo NO tocar este backend:**
- Lógica de transición cuando el conductor **reenvía** un doc rechazado → es **Carpool backend** (`DriverService.updateMe`), no este. Este solo lee el resultado.
- Lógica de cómo se inicializa `documentStatuses` al registrar un driver → **Carpool backend** (`DriverService.register`).

## Notificaciones cross-service (Admin → usuario)

- El motor vive en **Carpool** (`mx.colectivo.api.service.NotificationService`, colección Mongo `notifications`). Admin y Carpool comparten la **misma base `colectivo`**.
- En vez de endpoint HTTP, este backend **escribe directo** en `notifications`: `model/Notification.java` (mismo esquema que Carpool; `type` como String, p.ej. `"VERIFICATION"`), `repository/NotificationRepository.java` (solo escritura), `service/UserNotificationService.java` (`notifyVerificationApproved`/`notifyVerificationRejected`, tolerante a fallos, con deep-link). Wiring en `VerificationService.approve()`/`reject()`.
- **Tradeoff**: solo entrega in-app; si Carpool agrega FCM, migrar a endpoint interno o listener sobre `notifications`.

## Estructura git (importante)

- El repo git real es la carpeta `backend/`. La raíz `Colectivo Admin/` **no** es repo (git resuelve a un repo padre con config rota). Trabajar/commitear **siempre dentro de `backend/`**.
- Shell del usuario = PowerShell: usar `;` (no `&&`/`||`).
- `target/` ya no se versiona (`.gitignore` agregado); no re-trackear build ni `hs_err_pid*.log`.
