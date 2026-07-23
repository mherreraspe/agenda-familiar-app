import { abrirFlujoEventos } from './api'

export type RecursoSincronizacion = 'HOY' | 'AGENDA' | 'SALUD' | 'OBJETOS'
export type EstadoSincronizacion = 'conectado' | 'reconectando' | 'sin-conexion'

export interface EventoSincronizacion {
  id: string
  recursos: RecursoSincronizacion[]
}

interface EventoSseCrudo { id: string; tipo: string; datos: string }

export function crearRegistroEventos(limite = 200) {
  const vistos = new Set<string>()
  const orden: string[] = []
  return (id: string) => {
    if (!id || vistos.has(id)) return false
    vistos.add(id)
    orden.push(id)
    if (orden.length > limite) vistos.delete(orden.shift()!)
    return true
  }
}

export function demoraReconexion(intento: number, aleatorio = Math.random()) {
  return Math.min(30_000, 750 * (2 ** Math.min(intento, 6))) + Math.floor(aleatorio * 250)
}

export function extraerEventosSse(bloque: string): EventoSseCrudo[] {
  return bloque.split(/\r?\n\r?\n/).filter(Boolean).flatMap(fragmento => {
    let id = ''
    let tipo = 'message'
    const datos: string[] = []
    for (const linea of fragmento.split(/\r?\n/)) {
      if (linea.startsWith(':')) continue
      const separador = linea.indexOf(':')
      const campo = separador < 0 ? linea : linea.slice(0, separador)
      const valor = separador < 0 ? '' : linea.slice(separador + 1).replace(/^ /, '')
      if (campo === 'id') id = valor
      if (campo === 'event') tipo = valor
      if (campo === 'data') datos.push(valor)
    }
    return datos.length ? [{ id, tipo, datos: datos.join('\n') }] : []
  })
}

function esperar(ms: number, signal: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    const abortar = () => {
      window.clearTimeout(temporizador)
      reject(new DOMException('Cancelado', 'AbortError'))
    }
    const temporizador = window.setTimeout(() => {
      signal.removeEventListener('abort', abortar)
      resolve()
    }, ms)
    signal.addEventListener('abort', abortar, { once: true })
  })
}

export function suscribirEventosFamilia(
  alCambiar: (evento: EventoSincronizacion) => void | Promise<void>,
  alCambiarEstado: (estado: EstadoSincronizacion) => void
) {
  const cierre = new AbortController()
  let conexion: AbortController | null = null
  let ultimoEventoId = ''
  let intento = 0
  const recordar = crearRegistroEventos()

  const cambiarPorRed = () => {
    if (!navigator.onLine) {
      alCambiarEstado('sin-conexion')
      conexion?.abort()
    }
  }
  window.addEventListener('offline', cambiarPorRed)

  const ejecutar = async () => {
    while (!cierre.signal.aborted) {
      if (!navigator.onLine) {
        alCambiarEstado('sin-conexion')
        try { await esperar(1_000, cierre.signal) } catch { break }
        continue
      }
      conexion = new AbortController()
      const abortarConexion = () => conexion?.abort()
      cierre.signal.addEventListener('abort', abortarConexion, { once: true })
      try {
        if (intento) alCambiarEstado('reconectando')
        const respuesta = await abrirFlujoEventos(ultimoEventoId, conexion.signal)
        alCambiarEstado('conectado')
        intento = 0
        const lector = respuesta.body!.getReader()
        const decodificador = new TextDecoder()
        let pendiente = ''
        while (!cierre.signal.aborted) {
          const { done, value } = await lector.read()
          pendiente += decodificador.decode(value, { stream: !done })
          pendiente = pendiente.replaceAll('\r\n', '\n')
          const limite = pendiente.lastIndexOf('\n\n')
          if (limite >= 0) {
            const procesable = pendiente.slice(0, limite + 2)
            pendiente = pendiente.slice(limite + 2)
            for (const crudo of extraerEventosSse(procesable)) {
              if (crudo.id) ultimoEventoId = crudo.id
              if ((crudo.tipo === 'cambio' || crudo.tipo === 'sincronizar') && recordar(crudo.id)) {
                await alCambiar(JSON.parse(crudo.datos) as EventoSincronizacion)
              }
            }
          }
          if (done) break
        }
      } catch (error) {
        if (cierre.signal.aborted) break
        if (!navigator.onLine) alCambiarEstado('sin-conexion')
        else alCambiarEstado('reconectando')
      } finally {
        cierre.signal.removeEventListener('abort', abortarConexion)
      }
      intento += 1
      const demora = demoraReconexion(intento)
      try { await esperar(demora, cierre.signal) } catch { break }
    }
  }

  void ejecutar()
  return () => {
    cierre.abort()
    conexion?.abort()
    window.removeEventListener('offline', cambiarPorRed)
  }
}
