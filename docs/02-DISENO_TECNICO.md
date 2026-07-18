# Diseño técnico — Agenda Familiar

**Versión:** 0.1
**Estado:** propuesta para revisión
**Arquitectura:** frontend Vue + servicio de autenticación + monolito modular de negocio + PostgreSQL

## 1. Decisiones principales

- Frontend: Vue 3, TypeScript, Vite, Vue Router y PWA.
- Backend: dos procesos Java 25 LTS con Spring Boot 4.1: autenticación y negocio familiar.
- Persistencia: PostgreSQL 18 y migraciones Flyway.
- Autenticación: servicio independiente con Spring Security, credenciales, segundo factor y dispositivos confiables.
- Tiempo real: Server-Sent Events; Web Push para aplicación cerrada.
- Proxy y TLS: Nginx existente.
- Despliegue: Docker Compose con backend y PostgreSQL; Nginx permanece nativo.
- Topología: una instancia multi-tenant para varias familias, con aislamiento obligatorio por `family_id`.
- Capacidad inicial de diseño: hasta 20 familias activas; no constituye un límite rígido.
- Dominio objetivo: `familia.obusystem.com`; `obusystem.com` podrá redirigir cuando el DNS esté listo.

La documentación oficial lista Spring Boot 4.1 como estable y compatible con Java hasta la versión 26. Java 25 es LTS. PostgreSQL 18 es la versión estable actual y tiene soporte hasta 2030.

## 2. Estado del servidor y preparación

- Ubuntu 22.04 ARM64, 4 CPU, 23 GiB RAM.
- PostgreSQL instalado: 14.23; debe migrarse a PostgreSQL 18 antes de datos reales.
- Java no está instalado en el host; el runtime se incluirá en la imagen del backend.
- Docker 29, Nginx, swap de 4 GB y Fail2ban disponibles.
- El servidor no conserva aplicaciones ni bases de negocio anteriores.

PostgreSQL 14 no se actualizará “encima”. Se creará un volumen limpio PostgreSQL 18 en Docker y se validará con Flyway. El PostgreSQL nativo 14 se detendrá después de confirmar que no tiene datos de negocio.

## 3. Arquitectura

```text
PWA Vue en Android/iPhone
        │ HTTPS
        ▼
Nginx :443
  ├── /api/v1/autenticacion/* → servicio-autenticacion :8101
  └── /api/v1/*               → servicio-agenda :8102
                                      │
                                      ├── SSE y Web Push
                                      ├── PostgreSQL 18
                                      └── archivos cifrados
```

El build de Vue será un artefacto versionado servido por Nginx. Los dos servicios Java tendrán imágenes y ciclos de despliegue independientes. Nginx presentará un único origen al navegador para evitar CORS innecesario.

El servicio de autenticación poseerá usuarios, contraseñas, segundo factor, dispositivos confiables, sesiones/refresh tokens y roles globales. El servicio de agenda poseerá familias, membresías y permisos del dominio. Un token firmado identifica al usuario; el servicio de agenda valida la firma localmente y vuelve a comprobar la membresía en su propia base.

Ambos servicios compartirán el clúster PostgreSQL 18, pero usarán bases y credenciales separadas: `autenticacion` y `agenda_familiar`. Ningún servicio realizará consultas directas sobre las tablas del otro. La comunicación necesaria se hará mediante contratos HTTP y eventos internos explícitos.

## 4. Módulos del monolito

- `autenticacion` (servicio independiente): usuarios, contraseñas, segundo factor, tokens y dispositivos confiables.
- `plataforma`: administración de familias, cuotas y capacidad global sin acceso ordinario al contenido clínico.
- `family`: familias, adultos, dependientes y membresías.
- `medicine`: catálogo, envases, existencias y vencimientos.
- `treatment`: tratamientos, horarios y ocurrencias.
- `agenda`: citas, eventos, tareas y recurrencias.
- `review`: elementos vencidos y decisiones de cierre/reprogramación.
- `notification`: preferencias, Web Push, reintentos y escalamiento.
- `media`: validación, compresión, cifrado, cuota y descarga autorizada.
- `audit`: historial inmutable de operaciones sensibles.
- `backup`: coordinación y comprobación de respaldos.

Los módulos se separarán por paquetes y servicios, pero se desplegarán como un único proceso. No habrá microservicios, Redis, Kafka ni Kubernetes.

## 5. Modelo de datos inicial

Tablas principales:

- `families`: configuración, zona horaria y cuota.
- `users`: credenciales y estado.
- `family_memberships`: relación usuario-familia y rol.
- `profiles`: adulto o dependiente.
- `trusted_devices` y tablas Spring Session.
- `medications`: nombre, presentación y concentración textual.
- `medicine_batches`: cantidad, unidad, vencimiento y estado.
- `treatments`: indicación, fechas, responsable y estado.
- `treatment_schedules`: reglas horarias.
- `dose_occurrences`: ocurrencias materializadas y resolución.
- `events`: citas y recurrencias.
- `tasks`: tareas, fecha y estado.
- `review_items`: motivo, referencia y resolución.
- `attachments`: propietario, hash, tamaño, dimensiones, ruta cifrada y estado.
- `push_subscriptions`: dispositivo y endpoint cifrado.
- `audit_entries`: actor, operación, entidad, fecha y resumen seguro.

Todas las entidades familiares incluyen `family_id`, `created_at`, `updated_at` y `version` para control optimista. Los IDs públicos serán UUIDv7; las claves internas pueden permanecer `bigint` si las mediciones muestran una ventaja.

El aislamiento se aplicará en dos niveles: filtros obligatorios en repositorios/servicios Spring y Row Level Security de PostgreSQL como defensa adicional. Cada transacción establecerá el contexto de familia y las pruebas intentarán accesos cruzados de forma explícita.

## 6. API inicial

- `POST /api/v1/autenticacion/iniciar-sesion`, `POST /cerrar-sesion`, `POST /renovar` y `GET /usuario-actual`.
- `/api/v1/platform/families` para administración global restringida.
- `/api/v1/families/current` y `/memberships` para seleccionar una membresía autorizada.
- `/api/v1/family` y `/profiles`.
- `/api/v1/medications` y `/medicine-batches`.
- `/api/v1/treatments`, `/schedules` y `/occurrences`.
- `/api/v1/events` y `/tasks`.
- `/api/v1/review`.
- `/api/v1/attachments` para carga multipart y descarga autenticada.
- `/api/v1/push/subscriptions` y `/preferences`.
- `GET /api/v1/events/stream` para SSE.

La API usará JSON, errores RFC 9457 `ProblemDetail`, paginación por cursor y claves de idempotencia en creación de ocurrencias y confirmaciones.

## 7. Seguridad

- Refresh token rotatorio en cookie `HttpOnly`, `Secure` y `SameSite=Lax`; ningún token se guarda en `localStorage`.
- Access token firmado, de vida corta, mantenido únicamente en memoria por Vue.
- El servicio de agenda valida access tokens mediante clave pública y no consulta autenticación en cada petición.
- CSRF para operaciones mutables.
- Contraseñas con Argon2id.
- Segundo factor en un dispositivo nuevo; dispositivo confiable revocable.
- Autorización obligatoria por `family_id` y rol en servicio y repositorio.
- PostgreSQL Row Level Security para tablas familiares críticas.
- Rate limiting en Nginx y autenticación.
- Secretos fuera del repositorio y permisos de archivo restrictivos.
- Logs sin nombres de medicamentos, recetas, tokens o contenido de imágenes.
- Backups cifrados y prueba periódica de restauración.

## 8. Fotografías y almacenamiento

### Flujo

1. Vue comprueba formato, dimensiones y tamaño.
2. Canvas crea una copia reducida y elimina EXIF.
3. Se sube por multipart con límite de 2 MB por archivo procesado.
4. Java decodifica la imagen con ImageIO/TwelveMonkeys; no confía en extensión ni MIME declarado.
5. Java normaliza orientación, dimensiones y formato.
6. Se calcula SHA-256 para deduplicación dentro de la familia.
7. El archivo normalizado se cifra individualmente con AES-256-GCM.
8. PostgreSQL guarda metadatos, nonce, hash y ruta opaca.

### Ubicación

```text
/srv/agenda-familiar/media/<family-uuid>/<yyyy>/<mm>/<uuid>.bin
```

La ruta no se publica en Nginx. Spring Boot descifra en streaming después de comprobar permisos. Los archivos temporales se crean en un directorio con cuota y se eliminan en éxito o error.

### Límites

- Tipos aceptados de entrada: JPEG, PNG, HEIC/HEIF cuando el navegador pueda convertirlo.
- Salida preferida: WebP; JPEG normalizado como fallback de compatibilidad.
- Fotografía: máximo 1,600 px y 500 KB objetivo.
- Documento: máximo 2,400 px y 1 MB objetivo.
- Cuota predeterminada por familia: 1 GiB, configurable en bytes por el administrador.
- Avisos: 70 %, 85 % y 95 %; bloqueo de carga al 100 %.
- Deduplicación limitada a la misma familia para no filtrar coincidencias entre familias.

La reserva de cuota será transaccional: antes de aceptar un archivo se reservarán sus bytes en PostgreSQL y la reserva se liberará si el procesamiento falla. Así, dos cargas simultáneas no pueden superar el límite. Existirá además un límite global de emergencia que bloqueará nuevas cargas cuando el disco libre baje del umbral operativo.

El original no se sube. Una fase posterior añadirá una interfaz `MediaStorage` con implementaciones local, S3 y WebDAV.

## 9. Notificaciones y sincronización

- Spring Scheduler consulta recordatorios vencidos en lotes pequeños.
- PostgreSQL bloquea filas con `FOR UPDATE SKIP LOCKED` para evitar dobles envíos.
- Tabla de entregas con clave idempotente y estados pendiente/enviado/fallido.
- Web Push usa VAPID y contenido genérico en pantalla bloqueada.
- SSE notifica cambios a sesiones abiertas; el cliente vuelve a consultar la entidad.
- Si SSE se interrumpe, Vue reconecta con retroceso exponencial.
- No se implementará escritura offline en el MVP; la PWA mostrará caché de lectura y un aviso claro.

## 10. Backups

- `pg_dump` diario en formato custom, retención local de 14 días.
- Backup incremental de archivos por hash; no duplicar toda la carpeta cada día.
- Manifiesto relacionando dump, archivos, versiones y SHA-256.
- Cifrado antes de copiar fuera del servidor.
- Prueba mensual automatizada en base y carpeta temporales.
- La recuperación exige base, archivos, clave de medios, secretos VAPID y configuración.

## 11. Recursos y operación

- Servicio de autenticación: límite inicial 768 MiB; heap máximo aproximado de 512 MiB.
- Servicio de agenda: límite inicial 1.5 GiB; heap máximo aproximado de 1 GiB.
- PostgreSQL: límite inicial 2 GiB y conexiones Hikari pequeñas.
- Nginx: archivos estáticos y proxy.
- Fotografías procesadas con concurrencia máxima de dos trabajos.
- Presupuesto inicial: 1 GiB por familia y hasta 20 familias; se reservarán al menos 30 GiB para sistema/base/logs y 40 GiB para backups y temporales.
- Actuator expone únicamente `health`, `info` y métricas internas protegidas.
- Logs JSON con rotación; alertas por disco, errores de backup, push y cuota.

El objetivo es operar con menos de 5 GiB sostenidos, dejando margen amplio en los 23 GiB del servidor.

## 12. Estrategia de pruebas

- Backend: JUnit, Spring Boot Test, Testcontainers PostgreSQL 18 y ArchUnit.
- Frontend: Vitest, Vue Test Utils y Playwright móvil.
- Contratos: OpenAPI generado y validado en CI.
- Seguridad: pruebas de aislamiento por familia, CSRF, IDOR, cargas maliciosas y cuotas.
- Multi-tenancy: pruebas RLS, cambio de familia, invitaciones y concurrencia de cuota.
- Medios: legibilidad, EXIF eliminado, bomba de descompresión, duplicados y recuperación.
- Operación: restore completo, caída de Web Push, reconexión SSE y reinicio durante scheduler.

## 13. Fases técnicas

1. Repositorio, CI, Compose, PostgreSQL 18 y esqueletos Vue/Spring.
2. Servicio de autenticación, familia, permisos y auditoría.
3. Agenda, tareas y bandeja “Revisar”.
4. Botiquín, tratamientos y ocurrencias.
5. Fotografías, cuota y backups.
6. Web Push, SSE y endurecimiento.
7. Pruebas de aceptación, piloto y APK Android TWA.

## 14. Fuentes de versiones

- Spring Boot: <https://docs.spring.io/spring-boot/system-requirements.html>
- Java LTS: <https://www.oracle.com/java/technologies/java-se-support-roadmap.html>
- Vue: <https://vuejs.org/guide/quick-start.html>
- PostgreSQL: <https://www.postgresql.org/docs/current/index.htm>
- Política de versiones PostgreSQL: <https://www.postgresql.org/support/versioning/>

## 15. Checklist de aprobación técnica

- [ ] Stack y versiones aprobados.
- [ ] Monolito modular aprobado.
- [ ] Servicio de autenticación independiente aprobado.
- [ ] Multi-tenancy y aislamiento por familia aprobados.
- [ ] PostgreSQL 18 en Docker aprobado.
- [ ] Sesiones con cookie y segundo factor aprobados.
- [ ] Esquema inicial y API aprobados.
- [ ] Flujo de imágenes, cifrado y cuota aprobados.
- [ ] Estrategia de backup y restauración aprobada.
- [ ] Límites de recursos aprobados.

## 16. Repositorio y convenciones en español

El proyecto será un monorepositorio Git mientras exista un solo equipo:

```text
agenda-familiar-app/
  docs/
  frontend/
  servicios/autenticacion/
  servicios/agenda/
  infraestructura/
```

Convenciones propias:

- Clases Java: `Familia`, `MiembroFamilia`, `Medicamento`, `LoteMedicamento`, `Tratamiento`, `TomaProgramada`.
- Métodos y variables: `buscarFamilia`, `fechaVencimiento`, `miembrosActivos`.
- Tablas: `familias`, `miembros_familia`, `medicamentos`, `lotes_medicamento`, `tratamientos`, `tomas_programadas`.
- Columnas: `familia_id`, `fecha_vencimiento`, `creado_en`, `actualizado_en`.
- Endpoints: `/familias`, `/medicamentos`, `/tratamientos`, `/tomas-programadas`.
- Enumeraciones: `PENDIENTE`, `TOMADA`, `OMITIDA`, `REPROGRAMADA`.
- Componentes Vue: `PantallaHoy.vue`, `TarjetaTratamiento.vue`, `ListaPendientes.vue`.

Los identificadores no usarán tildes, `ñ` ni espacios. Se mantienen nombres estándar externos como `application.yml`, `SPRING_DATASOURCE_URL`, encabezados HTTP, claims OIDC (`sub`, `iss`, `aud`) y APIs de bibliotecas. Los commits usarán mensajes en español con Conventional Commits, por ejemplo `feat(botiquin): registrar lote de medicamento`.
