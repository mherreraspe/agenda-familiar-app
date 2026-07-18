# Infraestructura

El archivo `compose.yaml` levanta PostgreSQL 18 con bases y credenciales separadas para autenticación y agenda, ambos servicios Java y el frontend. Copia `.env.example` como `.env` y reemplaza todas las contraseñas antes de usar un entorno compartido.

```bash
docker compose -f infraestructura/compose.yaml up --build
```

Los datos quedan en un volumen Docker. Nginx permanece fuera de Compose conforme al diseño técnico; `nginx/agenda-familiar.conf` publica el sistema en `https://www.obusystem.com` y redirige el dominio raíz.
