# Infraestructura

El archivo `compose.yaml` levanta PostgreSQL 18 con bases y credenciales separadas para autenticación y agenda, además de ambos servicios Java. Copia `.env.example` como `.env` y reemplaza las contraseñas antes de usar un entorno compartido.

```bash
docker compose -f infraestructura/compose.yaml up --build
```

Los datos quedan en un volumen Docker. Nginx permanece fuera de Compose conforme al diseño técnico.
