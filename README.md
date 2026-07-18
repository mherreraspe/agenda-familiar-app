# Agenda Familiar OBU System

Plataforma PWA multi-familia para botiquín, tratamientos, citas, tareas y recordatorios.

## Estructura

- `docs/`: especificación funcional y diseño técnico.
- `frontend/`: PWA Vue 3.
- `servicios/autenticacion/`: servicio Java de identidad y acceso.
- `servicios/agenda/`: backend Java del dominio familiar.
- `infraestructura/`: Docker Compose, Nginx, backups y despliegue.

El código utiliza nombres de dominio en español. Yuvomi se conserva fuera de este repositorio únicamente como referencia de investigación y no forma parte del producto.

## Desarrollo local

Requisitos: Node.js 24 o superior, Java 25, Maven 3.9 y Docker con Compose.

```bash
# Frontend
cd frontend
npm install
npm run dev

# Backend completo
mvn verify

# Infraestructura y servicios
docker compose -f infraestructura/compose.yaml up --build
```

Los archivos `.env.example` documentan las variables requeridas. Ningún secreto real debe entrar al repositorio.
