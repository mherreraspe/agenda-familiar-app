# Servicio de autenticación

Servicio Spring Boot independiente responsable de credenciales, segundo factor, dispositivos confiables y tokens firmados.

Expone inicialmente el contexto `/api/v1/autenticacion` y el endpoint protegido de salud de Actuator. Las migraciones Flyway pertenecen exclusivamente a la base `autenticacion`.
