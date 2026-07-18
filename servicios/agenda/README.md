# Servicio de agenda

Monolito modular Spring Boot responsable de familias, botiquín, tratamientos, calendario, tareas, archivos, notificaciones y auditoría.

La primera migración crea el núcleo multi-tenant (`familias`, `miembros_familia` y `perfiles`) y habilita Row Level Security para que las siguientes fases partan de aislamiento por familia.
