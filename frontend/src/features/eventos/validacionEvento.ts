import { fechaFamiliarAInstant } from '../../utils/fechaFamiliar'

export interface BorradorEvento {
  perfilId: string
  titulo: string
  tipo: string
  lugar: string
  direccion: string
  notas: string
  inicioEn: string
  finEn: string
  avisar24h: boolean
  avisar1h: boolean
  repetir: boolean
  frecuencia: 'DIARIA' | 'SEMANAL' | 'MENSUAL'
  intervalo: number
  hasta: string
}

export type CampoEvento = 'titulo' | 'inicioEn' | 'finEn' | 'hasta'
export type ErroresEvento = Partial<Record<CampoEvento, string>>

export function validarEvento(borrador: BorradorEvento, zonaHoraria: string, ahora = new Date()) {
  const errores: ErroresEvento = {}
  if (!borrador.titulo.trim()) errores.titulo = 'Escribe un título.'

  let inicio: string | undefined
  if (!borrador.inicioEn) {
    errores.inicioEn = 'Elige la fecha y hora de inicio.'
  } else {
    try {
      inicio = fechaFamiliarAInstant(borrador.inicioEn, zonaHoraria)
      if (new Date(inicio).getTime() <= ahora.getTime()) errores.inicioEn = 'El inicio debe estar en el futuro.'
    } catch (causa) {
      errores.inicioEn = causa instanceof Error ? causa.message : 'La fecha de inicio no es válida.'
    }
  }

  if (borrador.finEn) {
    try {
      const fin = fechaFamiliarAInstant(borrador.finEn, zonaHoraria)
      if (inicio && new Date(fin).getTime() <= new Date(inicio).getTime()) {
        errores.finEn = 'El fin debe ser posterior al inicio.'
      }
    } catch (causa) {
      errores.finEn = causa instanceof Error ? causa.message : 'La fecha de fin no es válida.'
    }
  }

  if (borrador.repetir) {
    if (!borrador.hasta) {
      errores.hasta = 'Elige cuándo termina la repetición.'
    } else {
      try {
        const hasta = fechaFamiliarAInstant(borrador.hasta, zonaHoraria)
        if (inicio && new Date(hasta).getTime() <= new Date(inicio).getTime()) {
          errores.hasta = 'El fin de la repetición debe ser posterior al primer evento.'
        }
      } catch (causa) {
        errores.hasta = causa instanceof Error ? causa.message : 'El fin de la repetición no es válido.'
      }
    }
  }

  return errores
}
