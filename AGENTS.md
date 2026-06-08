# Colectivo Admin Backend — Agents Guide

> Este archivo es el mismo contexto que `CLAUDE.md` (intencionalmente duplicado
> para que cualquier agente — Codex, Claude, otros — lo encuentre con la
> convención de nombre que use). Léelo completo antes de tocar código.

## Punto crítico de este backend

- `VerificationService.approve()` aprueba TODOS los required documents y
  borra `rejectedFields`/`verificationNote`. La fuente canónica per-documento
  es el Map `documentStatuses`; computa `verificationStatus` derivado.
- `VerificationService.reject()` valida que `rejectedFields` no esté vacío,
  toma `approvedFields` opcional, y produce `documentStatuses` con los 4
  required fields seteados explícitamente (approved/rejected/pending).
- `DriverVerification.java` usa `@Field` para remapear: `vehicleMakeName`
  (cómo guarda Carpool) → propiedad `marca` (cómo usa este backend y el
  frontend admin). No renombrar `marca/modelo/anio` — DTO y UI dependen.
- `verificationStatus` enum lowercase: `pending` | `approved` | `rejected`.
  `.name()` se serializa tal cual al DTO.
- `REQUIRED_DOCUMENT_FIELDS` = `["platePhoto", "vehiclePhoto", "licFront", "licBack"]`.
  Si agregas otro doc requerido, actualiza esta lista y revisa
  `computeVerificationStatus` + `normalizeDocumentStatuses`.

## Lo que NO escribe este backend

- Estructura inicial del driver (plate, capacity, fotos, capacidad). Eso lo
  escribe Carpool en `POST /api/driver/register`. Este backend solo escribe
  los campos de verificación.
- `documentStatuses` cuando el conductor reenvía. Carpool maneja esa
  transición (deja en `pending` el reenviado, mantiene approved los demás).
  Este backend lee el resultado.

---

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

**Cuándo NO tocar este backend:**
- Lógica de transición cuando el conductor **reenvía** un doc rechazado → es **Carpool backend** (`DriverService.updateMe`), no este. Este solo lee el resultado.
- Lógica de cómo se inicializa `documentStatuses` al registrar un driver → **Carpool backend** (`DriverService.register`).
