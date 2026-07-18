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
}

export interface RespuestaHoy {
  familiaId: string
  familia: string
  zonaHoraria: string
  perfiles: PerfilResumen[]
  tareas: TareaResumen[]
}

export interface RespuestaCatalogo {
  medicamentos: Array<{ id: string; nombre: string; presentacion: string; concentracion: string; cantidad: number; unidad: string; fechaVencimiento?: string; estado: string }>
  tratamientos: Array<{ id: string; perfilId: string; persona: string; medicamentoId?: string; medicamento: string; indicacion?: string; dosisIndicada?: string; frecuencia?: string; fechaInicio: string; fechaFin?: string; estado: string }>
  eventos: Array<{ id: string; perfilId?: string; persona?: string; titulo: string; tipo?: string; lugar?: string; inicioEn: string; finEn?: string; estado: string }>
}

export type EstadoOcurrencia = 'PENDIENTE' | 'TOMADA' | 'OMITIDA' | 'POSPUESTA' | 'CANCELADA'

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
  if (opciones.body) encabezados.set('Content-Type', 'application/json')
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

export function completarTarea(tareaId: string) {
  return solicitud<TareaResumen>(`/api/v1/familias/${FAMILIA_TEST_ID}/tareas/${tareaId}/estado/COMPLETADA`, { method: 'PATCH' })
}

export function crearTarea(datos: { titulo: string; descripcion: string; perfilId: string; fechaLimite: string }) {
  return solicitud<TareaResumen>(`/api/v1/familias/${FAMILIA_TEST_ID}/tareas`, { method: 'POST', body: JSON.stringify(datos) })
}

export function crearEvento(datos: { perfilId?: string; titulo: string; tipo?: string; lugar?: string; direccion?: string; notas?: string; inicioEn: string; finEn?: string }) {
  return solicitud<{ id: string }>(`/api/v1/familias/${FAMILIA_TEST_ID}/eventos`, { method: 'POST', body: JSON.stringify(datos) })
}

export function crearMedicamento(datos: { nombre: string; presentacion: string; concentracion: string; cantidad: number; unidad: string; fechaVencimiento?: string }) {
  return solicitud<{ id: string }>(`/api/v1/familias/${FAMILIA_TEST_ID}/medicamentos`, { method: 'POST', body: JSON.stringify(datos) })
}

export function crearTratamiento(datos: { perfilId: string; medicamentoId?: string; nombre: string; indicacion?: string; cantidadReceta?: string; frecuencia?: string; horario: string; fechaInicio?: string; fechaFin?: string; responsablePerfilId?: string }) {
  return solicitud<{ id: string }>(`/api/v1/familias/${FAMILIA_TEST_ID}/tratamientos`, { method: 'POST', body: JSON.stringify(datos) })
}
