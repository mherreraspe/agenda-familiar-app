export const FAMILIA_TEST_ID = '0197f100-0000-7000-8000-000000000001'

let accessToken = ''
let renovacionEnCurso: Promise<RespuestaSesion> | null = null

export interface PerfilResumen {
  id: string
  nombre: string
  tipo: 'ADULTO' | 'DEPENDIENTE'
  color: string
  relacion: string
}

export interface TareaResumen {
  id: string
  titulo: string
  descripcion?: string
  fechaLimite: string
  estado: 'PENDIENTE' | 'COMPLETADA' | 'OMITIDA' | 'REPROGRAMADA'
  perfilId: string
  responsable: string
  recurrente: boolean
  tareaOrigenId?: string
}

export interface RespuestaHoy {
  familiaId: string
  familia: string
  zonaHoraria: string
  perfiles: PerfilResumen[]
  tareas: TareaResumen[]
}

export type EstadoBotiquin = 'DISPONIBLE' | 'POR_VENCER' | 'VENCIDO' | 'AGOTADO' | 'DESCARTADO'

export interface RespuestaCatalogo {
  medicamentos: Array<{ id: string; loteId?: string; nombre: string; presentacion: string; concentracion: string; cantidad: number; unidad: string; fechaVencimiento?: string; estado: EstadoBotiquin }>
  tratamientos: Array<{ id: string; perfilId: string; persona: string; medicamentoId?: string; medicamento: string; responsablePerfilId: string; responsable: string; responsableAlternativoPerfilId?: string; responsableAlternativo?: string; indicacion?: string; dosisIndicada?: string; frecuencia?: string; horarios: string[]; intervaloHoras?: number; fechaInicio: string; fechaFin?: string; estado: string; recetaId?: string }>
  eventos: Array<{ id: string; perfilId?: string; persona?: string; titulo: string; tipo?: string; lugar?: string; direccion?: string; notas?: string; inicioEn: string; finEn?: string; estado: string; recurrente: boolean; eventoOrigenId?: string }>
  lugares: Array<{ id: string; nombre: string; direccion?: string; ultimaUtilizacion: string; frecuenciaUso: number }>
}

export interface EntradaAuditoria {
  operacion: string
  entidad: string
  entidadId: string
  titulo: string
  actorId: string
  actor: string
  resumen: string
  fecha: string
}

export interface RespuestaAuditoria { entradas: EntradaAuditoria[] }
export interface SugerenciaFamiliar { tipo: 'EVENTO' | 'LUGAR'; entidadId: string; titulo: string; lugar?: string; direccion?: string }
export interface RespuestaSugerencias { sugerencias: SugerenciaFamiliar[] }

export type EstadoOcurrencia = 'PENDIENTE' | 'TOMADA' | 'OMITIDA' | 'POSPUESTA' | 'REPROGRAMADA' | 'CANCELADA'

export interface OcurrenciaResumen {
  id: string
  tratamientoId: string
  perfilId: string
  persona: string
  tratamiento: string
  programadaEn: string
  estado: EstadoOcurrencia
  pospuestaA?: string
  resueltaPor?: string
  resueltaPorNombre?: string
  resueltaEn?: string
}

export interface ElementoRevision {
  id: string
  origen: 'OCURRENCIA' | 'TRATAMIENTO' | 'LOTE_MEDICAMENTO'
  entidadId: string
  motivo: string
  titulo: string
  fecha?: string
  estado: 'PENDIENTE' | 'RESUELTO'
}

export interface RespuestaOcurrencias {
  ocurrencias: OcurrenciaResumen[]
  revisar: ElementoRevision[]
}

export type FrecuenciaRecurrencia = 'DIARIA' | 'SEMANAL' | 'MENSUAL'
export interface RecurrenciaSolicitud { frecuencia: FrecuenciaRecurrencia; intervalo: number; hasta: string }
export interface PerfilAdministrado { id: string; nombre: string; tipo: 'ADULTO' | 'DEPENDIENTE'; color?: string; relacion?: string; usuarioId?: string; permiso?: 'ADMINISTRADOR_FAMILIAR' | 'ADULTO'; activo: boolean }
export interface RespuestaFamilia { puedeAdministrar: boolean; perfiles: PerfilAdministrado[] }
export type DatosPerfil = Omit<PerfilAdministrado, 'id'>
export interface RespuestaCuota { cuotaBytes: number; usadosBytes: number; disponiblesBytes: number; porcentaje: number; nivel: 'NORMAL' | 'MEDIA' | 'ALTA' | 'CRITICA' | 'BLOQUEADA' }
export interface RespuestaArchivo { id: string; tratamientoId: string; ancho: number; alto: number; bytesAlmacenados: number; creadoEn: string }

interface RespuestaSesion {
  accessToken: string
  expiraEn: string
  usuarioId: string
  correo: string
}

function leerCookie(nombre: string) {
  const prefijo = `${encodeURIComponent(nombre)}=`
  const valor = document.cookie.split('; ').find(cookie => cookie.startsWith(prefijo))?.slice(prefijo.length)
  return valor ? decodeURIComponent(valor) : ''
}

async function problema(respuesta: Response) {
  const detalle = await respuesta.json().catch(() => null) as { detail?: string; title?: string } | null
  return new Error(detalle?.detail ?? detalle?.title ?? 'No se pudo completar la operación')
}

async function pedirSesion(ruta: string, opciones: RequestInit = {}) {
  const respuesta = await fetch(ruta, { ...opciones, credentials: 'same-origin' })
  if (!respuesta.ok) throw await problema(respuesta)
  const sesion = await respuesta.json() as RespuestaSesion
  accessToken = sesion.accessToken
  return sesion
}

export async function renovarSesion() {
  if (!renovacionEnCurso) {
    renovacionEnCurso = pedirSesion('/api/v1/autenticacion/renovar', {
      method: 'POST',
      headers: { 'X-XSRF-TOKEN': leerCookie('XSRF-TOKEN') }
    }).finally(() => { renovacionEnCurso = null })
  }
  return renovacionEnCurso
}

async function solicitud<T>(ruta: string, opciones: RequestInit = {}, reintentar = true): Promise<T> {
  const encabezados = new Headers(opciones.headers)
  encabezados.set('Accept', 'application/json')
  if (opciones.body && !(opciones.body instanceof FormData)) encabezados.set('Content-Type', 'application/json')
  if (accessToken) encabezados.set('Authorization', `Bearer ${accessToken}`)

  const respuesta = await fetch(ruta, { ...opciones, headers: encabezados, credentials: 'same-origin' })
  if (respuesta.status === 401 && reintentar && accessToken) {
    await renovarSesion()
    return solicitud<T>(ruta, opciones, false)
  }
  if (!respuesta.ok) throw await problema(respuesta)
  if (respuesta.status === 204) return undefined as T
  return respuesta.json() as Promise<T>
}

export async function iniciarSesion(correo: string, clave: string) {
  return pedirSesion('/api/v1/autenticacion/iniciar-sesion', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ correo, clave })
  })
}

export async function cerrarSesion() {
  const respuesta = await fetch('/api/v1/autenticacion/cerrar-sesion', {
    method: 'POST', credentials: 'same-origin',
    headers: { 'X-XSRF-TOKEN': leerCookie('XSRF-TOKEN') }
  })
  if (!respuesta.ok) throw await problema(respuesta)
  accessToken = ''
}

export function consultarHoy() {
  return solicitud<RespuestaHoy>(`/api/v1/familias/${FAMILIA_TEST_ID}/hoy`)
}

export function consultarCatalogo() {
  return solicitud<RespuestaCatalogo>(`/api/v1/familias/${FAMILIA_TEST_ID}/catalogo`)
}

export function consultarOcurrencias() {
  return solicitud<RespuestaOcurrencias>(`/api/v1/familias/${FAMILIA_TEST_ID}/ocurrencias`)
}

export function consultarAuditoria() {
  return solicitud<RespuestaAuditoria>(`/api/v1/familias/${FAMILIA_TEST_ID}/auditoria`)
}

export function consultarConfiguracionFamilia() {
  return solicitud<RespuestaFamilia>(`/api/v1/familias/${FAMILIA_TEST_ID}/configuracion`)
}

export function consultarCuota() {
  return solicitud<RespuestaCuota>(`/api/v1/familias/${FAMILIA_TEST_ID}/archivos/cuota`)
}

export function consultarSugerencias(consulta: string) {
  return solicitud<RespuestaSugerencias>(`/api/v1/familias/${FAMILIA_TEST_ID}/sugerencias?q=${encodeURIComponent(consulta)}`)
}

export function cambiarEstadoOcurrencia(ocurrenciaId: string, estado: Exclude<EstadoOcurrencia, 'PENDIENTE'>, pospuestaA?: string) {
  return solicitud<OcurrenciaResumen>(`/api/v1/familias/${FAMILIA_TEST_ID}/ocurrencias/${ocurrenciaId}/estado/${estado}`, {
    method: 'PATCH',
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    body: pospuestaA ? JSON.stringify({ pospuestaA }) : undefined
  })
}

export function cerrarElementoRevision(elementoId: string) {
  return solicitud<void>(`/api/v1/familias/${FAMILIA_TEST_ID}/revisar/${elementoId}/cerrar`, {
    method: 'PATCH',
    headers: { 'Idempotency-Key': crypto.randomUUID() }
  })
}

export function cerrarTratamiento(tratamientoId: string, motivo?: string) {
  return solicitud<void>(`/api/v1/familias/${FAMILIA_TEST_ID}/tratamientos/${tratamientoId}/cerrar`, {
    method: 'PATCH',
    headers: { 'Idempotency-Key': crypto.randomUUID() },
    body: JSON.stringify({ motivo: motivo || undefined })
  })
}

export function completarTarea(tareaId: string) {
  return actuarAgenda('tareas', tareaId, 'COMPLETAR')
}

export function actuarAgenda(entidad: 'tareas' | 'eventos', id: string, accion: 'COMPLETAR' | 'OMITIR' | 'REPROGRAMAR', fechaNueva?: string) {
  return solicitud<{ id: string; origenId?: string; entidad: string; estado: string; fecha: string }>(`/api/v1/familias/${FAMILIA_TEST_ID}/${entidad}/${id}/acciones/${accion}`, {
    method: 'PATCH', headers: { 'Idempotency-Key': crypto.randomUUID() },
    body: fechaNueva ? JSON.stringify({ fechaNueva }) : undefined
  })
}

export function crearTarea(datos: { titulo: string; descripcion: string; perfilId: string; fechaLimite: string; recurrencia?: RecurrenciaSolicitud }) {
  return solicitud<TareaResumen>(`/api/v1/familias/${FAMILIA_TEST_ID}/tareas`, { method: 'POST', body: JSON.stringify(datos) })
}

export function crearEvento(datos: { perfilId?: string; titulo: string; tipo?: string; lugar?: string; direccion?: string; notas?: string; inicioEn: string; finEn?: string; recurrencia?: RecurrenciaSolicitud }) {
  return solicitud<{ id: string }>(`/api/v1/familias/${FAMILIA_TEST_ID}/eventos`, { method: 'POST', body: JSON.stringify(datos) })
}

export function crearPerfil(datos: DatosPerfil) {
  return solicitud<PerfilAdministrado>(`/api/v1/familias/${FAMILIA_TEST_ID}/configuracion/perfiles`, { method: 'POST', body: JSON.stringify(datos) })
}

export function actualizarPerfil(id: string, datos: DatosPerfil) {
  return solicitud<PerfilAdministrado>(`/api/v1/familias/${FAMILIA_TEST_ID}/configuracion/perfiles/${id}`, { method: 'PATCH', body: JSON.stringify(datos) })
}

export function crearMedicamento(datos: { nombre: string; presentacion: string; concentracion: string; cantidad: number; unidad: string; fechaVencimiento?: string }) {
  return solicitud<{ id: string }>(`/api/v1/familias/${FAMILIA_TEST_ID}/medicamentos`, { method: 'POST', body: JSON.stringify(datos) })
}

export function crearTratamiento(datos: { perfilId: string; medicamentoId?: string; nombre: string; indicacion?: string; cantidadReceta?: string; frecuencia?: string; horario: string; horarios?: string[]; intervaloHoras?: number; fechaInicio?: string; fechaFin?: string; responsablePerfilId?: string; responsableAlternativoPerfilId?: string }) {
  return solicitud<{ id: string }>(`/api/v1/familias/${FAMILIA_TEST_ID}/tratamientos`, { method: 'POST', body: JSON.stringify(datos) })
}

export function subirReceta(tratamientoId: string, archivo: Blob) {
  const formulario = new FormData()
  formulario.append('archivo', archivo, 'receta.jpg')
  return solicitud<RespuestaArchivo>(`/api/v1/familias/${FAMILIA_TEST_ID}/tratamientos/${tratamientoId}/receta`, {
    method: 'POST', body: formulario
  })
}

export function eliminarReceta(archivoId: string) {
  return solicitud<void>(`/api/v1/familias/${FAMILIA_TEST_ID}/archivos/${archivoId}`, { method: 'DELETE' })
}

export async function descargarReceta(archivoId: string, miniatura = false, reintentar = true): Promise<Blob> {
  const encabezados = new Headers({ Accept: 'image/jpeg' })
  if (accessToken) encabezados.set('Authorization', `Bearer ${accessToken}`)
  const respuesta = await fetch(`/api/v1/familias/${FAMILIA_TEST_ID}/archivos/${archivoId}?miniatura=${miniatura}`, {
    headers: encabezados, credentials: 'same-origin'
  })
  if (respuesta.status === 401 && reintentar && accessToken) {
    await renovarSesion()
    return descargarReceta(archivoId, miniatura, false)
  }
  if (!respuesta.ok) throw await problema(respuesta)
  return respuesta.blob()
}
