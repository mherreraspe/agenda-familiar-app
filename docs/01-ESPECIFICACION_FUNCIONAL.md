# Especificación funcional — Agenda Familiar

**Versión:** 0.1
**Estado:** propuesta para revisión
**Producto:** Agenda Familiar OBU System

## 1. Propósito

Agenda Familiar ayudará a varias familias independientes a recordar medicamentos, tratamientos, vencimientos, citas y tareas sin depender de formularios extensos. La experiencia principal debe sentirse como una agenda de mano: abrir, ver lo importante y actuar con uno o dos toques.

La aplicación organiza información; no diagnostica, prescribe ni sustituye indicaciones médicas.

## 2. Objetivos medibles

- Registrar una cita o tarea básica en menos de 30 segundos.
- Registrar un medicamento básico en menos de 45 segundos.
- Completar, omitir, cerrar o reprogramar un pendiente desde la pantalla principal.
- Mostrar en una sola vista lo correspondiente a hoy, lo atrasado y lo próximo.
- Compartir los cambios entre los miembros autorizados de la familia.
- Mantener notificaciones comprensibles y discretas.
- Permitir recuperar información después de cambiar o perder un teléfono.

## 3. Usuarios y permisos

### Administrador de la plataforma

- Crea, suspende y elimina espacios familiares.
- Asigna o modifica la cuota de almacenamiento de cada familia.
- Consulta únicamente métricas operativas y consumo, no contenido médico ordinario.
- Gestiona backups, capacidad y seguridad global.

### Administrador familiar

- Crea la familia y las cuentas de adultos.
- Administra dependientes y permisos.
- Configura almacenamiento, notificaciones y copias de seguridad.
- Puede consultar el historial de cambios.

### Adulto o cuidador

- Consulta el calendario familiar según sus permisos.
- Registra y administra tratamientos, citas y tareas.
- Confirma tomas o actividades.
- Puede recibir escalamiento de avisos.

### Dependiente

- Es un perfil asociado a tratamientos, citas y tareas.
- En el MVP no inicia sesión.
- Una cuenta propia para adolescentes queda fuera del MVP.

Una instalación puede alojar varias familias. Todas las consultas, archivos, notificaciones y eventos deben filtrarse por `family_id`. Un usuario puede pertenecer a una o más familias solo mediante una membresía explícita.

## 4. Principios de experiencia

- **Hoy primero:** la portada muestra solamente lo que requiere atención.
- **Captura progresiva:** primero título, persona y fecha; los detalles aparecen solo si son necesarios.
- **Pocas decisiones:** acciones principales visibles y lenguaje cotidiano.
- **Sin culpa:** los elementos atrasados se revisan, no se muestran como errores permanentes.
- **Accesible:** botones grandes, contraste suficiente y texto comprensible.
- **Privado:** las notificaciones bloqueadas no revelan nombres de medicamentos ni diagnósticos.

## 5. Alcance del MVP

### 5.1 Inicio “Hoy”

- Secciones: atrasado, hoy, próximos siete días y vencimientos cercanos.
- Filtros por miembro familiar.
- Acciones rápidas: completar, tomado, omitido, posponer, reprogramar y cerrar.
- Botón único `Agregar` con opciones: cita, tarea, medicamento o tratamiento.

### 5.2 Familia

- Crear perfiles de adultos y dependientes.
- Nombre visible, color/avatar y relación opcional.
- Permisos por rol.
- Dispositivos confiables y sesiones activas.

### 5.3 Botiquín

- Medicamento: nombre, presentación, concentración y notas.
- Lotes o envases: cantidad, unidad, fecha de vencimiento y fotografía opcional.
- Alertas configurables antes del vencimiento.
- Estados: disponible, por vencer, vencido, agotado y descartado.
- El sistema nunca calcula ni recomienda una dosis.

### 5.4 Tratamientos

- Persona, medicamento, indicación textual, dosis indicada, frecuencia, inicio y fin.
- Horarios simples o intervalos definidos por el usuario.
- Responsable principal y cuidador alternativo opcional.
- Registro de cada ocurrencia: pendiente, tomada, omitida, pospuesta o cancelada.
- Cierre anticipado con motivo opcional.

### 5.5 Calendario y tareas

- Citas médicas, odontológicas, oftalmológicas y actividades generales.
- Eventos de una vez o recurrentes.
- Recordatorios relativos: minutos, horas, días o meses antes.
- Tareas con responsable, fecha límite y recurrencia.
- Reprogramación sin perder el historial anterior.

### 5.6 Bandeja “Revisar”

- Reúne actividades vencidas, tomas sin confirmar, tratamientos finalizados y medicamentos vencidos.
- Cada elemento permite cerrar, confirmar, omitir o reprogramar.
- Los elementos no permanecen indefinidamente como pendientes invisibles.

### 5.7 Notificaciones

- Web Push para PWA instalada.
- Mensaje bloqueado discreto: “Tienes un recordatorio familiar”.
- El detalle aparece después de abrir la aplicación autenticada.
- Reintentos limitados y prevención de avisos duplicados.
- Escalamiento opcional al cuidador si una ocurrencia crítica sigue sin confirmarse.
- Horario silencioso configurable.

### 5.8 Fotografías

- Fotografía opcional para receta, etiqueta, medicamento o cita.
- El original permanece en el teléfono del usuario.
- Antes de subir, la PWA genera una copia reducida.
- Fotografías generales: máximo 1,600 px y objetivo de 200–500 KB.
- Documentos con texto: máximo 2,400 px y objetivo de 500 KB–1 MB.
- Se eliminan metadatos EXIF y ubicación.
- El usuario puede eliminar la fotografía conservando el registro.
- Cuota inicial por familia: 1 GB, modificable por el administrador de la plataforma, con avisos al 70 %, 85 % y 95 %.
- El consumo de una familia no reduce ni amplía automáticamente la cuota de otra.

## 6. Flujos principales

### Registrar una cita

1. Pulsar `Agregar` y elegir `Cita`.
2. Escribir el motivo o usar una sugerencia reciente.
3. Elegir persona, fecha y hora.
4. Opcionalmente agregar lugar, recordatorio, repetición o fotografía.
5. Guardar y volver a “Hoy”.

### Registrar un tratamiento

1. Elegir persona y medicamento.
2. Copiar exactamente la indicación de la receta.
3. Definir horarios y fechas.
4. Elegir responsable y escalamiento opcional.
5. Revisar el resumen antes de activar.

### Resolver un pendiente atrasado

1. Abrir “Revisar”.
2. Elegir `Hecho`, `Omitir`, `Cerrar` o `Reprogramar`.
3. Confirmar solo cuando la acción cambie información clínica relevante.

### Compartir información

1. Un adulto realiza un cambio.
2. El servidor valida permisos y guarda el historial.
3. Los demás dispositivos reciben actualización mediante SSE o en la siguiente consulta.
4. Si corresponde, reciben Web Push.

## 7. Reglas funcionales

- Una dosis siempre conserva el texto ingresado por el usuario; no se transforma en recomendación.
- No se puede eliminar definitivamente un tratamiento con ocurrencias sin dejar historial de auditoría.
- Las fechas se almacenan en UTC y se muestran en `America/Lima` por defecto.
- Los dependientes no poseen credenciales en el MVP.
- Las fotografías nunca son públicas ni accesibles por una URL predecible.
- Un adulto solo puede acceder a datos de su familia.
- Ningún identificador, búsqueda o descarga puede revelar la existencia de datos de otra familia.
- El borrado de una fotografía debe retirar también sus miniaturas.
- Los cambios concurrentes se resuelven con versión del registro y aviso de conflicto.

## 8. Fuera del MVP

- Diagnóstico, interacción de medicamentos o recomendación de dosis.
- OCR automático de recetas.
- Reconocimiento de voz.
- Integración con Google Calendar.
- Aplicación nativa para iOS.
- Publicación en Play Store o App Store.
- Almacenamiento externo Google Drive, WebDAV o S3.
- Facturación, cobros y autoservicio público para crear familias.

## 9. Criterios de aceptación

- Dos adultos pueden consultar los mismos cambios desde dispositivos distintos.
- Dos familias distintas no pueden consultar, inferir ni descargar información entre sí.
- Una cita básica se registra en menos de 30 segundos durante el piloto.
- Un elemento vencido siempre ofrece una salida clara.
- Una notificación bloqueada no expone información médica.
- Una imagen de receta permanece legible después de la compresión.
- La aplicación rechaza imágenes inválidas, enormes o de tipos no permitidos.
- Al alcanzar la cuota no se pierden registros: solo se bloquean nuevas fotografías.
- El backup permite restaurar base, archivos cifrados y configuración.
- Android y iPhone compatible pueden instalar la PWA y recibir Web Push.

## 10. Checklist de aprobación funcional

- [ ] Roles y permisos aprobados.
- [ ] Separación entre familias y administración de plataforma aprobadas.
- [ ] Pantalla “Hoy” aprobada.
- [ ] Flujos de botiquín y tratamiento aprobados.
- [ ] Reglas de notificación aprobadas.
- [ ] Política de fotografías y cuota aprobada.
- [ ] Funciones fuera del MVP aceptadas.
- [ ] Criterios de aceptación aceptados.
