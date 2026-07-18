export const FAMILIA_TEST_ID = '0197f100-0000-7000-8000-000000000001'

let accessToken = ''

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
  tratamientos: Array<{ id: string; perfilId: string; persona: string; medicamentoId: string; medicamento: string; indicacion: string; dosisIndicada: string; frecuencia: string; fechaInicio: string; fechaFin?: string; estado: string }>
  eventos: Array<{ id: string; perfilId: string; persona: string; titulo: string; tipo: string; lugar?: string; inicioEn: string; finEn?: string; estado: string }>
}

interface RespuestaSesion {
  accessToken: string
  expiraEn: string
  usuarioId: string
  correo: string
}

async function solicitud<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const encabezados = new Headers(opciones.headers)
  encabezados.set('Accept', 'application/json')
  if (opciones.body) encabezados.set('Content-Type', 'application/json')
  if (accessToken) encabezados.set('Authorization', `Bearer ${accessToken}`)

  const respuesta = await fetch(ruta, { ...opciones, headers: encabezados })
  if (!respuesta.ok) {
    const problema = await respuesta.json().catch(() => null) as { detail?: string; title?: string } | null
    throw new Error(problema?.detail ?? problema?.title ?? 'No se pudo completar la operación')
  }
  return respuesta.json() as Promise<T>
}

export async function iniciarSesion(correo: string, clave: string) {
  const sesion = await solicitud<RespuestaSesion>('/api/v1/autenticacion/iniciar-sesion', {
    method: 'POST',
    body: JSON.stringify({ correo, clave })
  })
  accessToken = sesion.accessToken
  return sesion
}

export function cerrarSesion() {
  accessToken = ''
}

export function consultarHoy() {
  return solicitud<RespuestaHoy>(`/api/v1/familias/${FAMILIA_TEST_ID}/hoy`)
}

export function consultarCatalogo() {
  return solicitud<RespuestaCatalogo>(`/api/v1/familias/${FAMILIA_TEST_ID}/catalogo`)
}

export function completarTarea(tareaId: string) {
  return solicitud<TareaResumen>(`/api/v1/familias/${FAMILIA_TEST_ID}/tareas/${tareaId}/estado/COMPLETADA`, {
    method: 'PATCH'
  })
}

export function crearTarea(datos: { titulo: string; descripcion: string; perfilId: string; fechaLimite: string }) {
  return solicitud<TareaResumen>(`/api/v1/familias/${FAMILIA_TEST_ID}/tareas`, {
    method: 'POST',
    body: JSON.stringify(datos)
  })
}
