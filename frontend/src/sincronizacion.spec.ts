import { describe, expect, it } from 'vitest'
import { crearRegistroEventos, demoraReconexion, extraerEventosSse } from './sincronizacion'

describe('sincronización familiar SSE', () => {
  it('interpreta eventos, comentarios y datos multilínea', () => {
    const eventos = extraerEventosSse([
      ': latido',
      '',
      'id: evento-1',
      'event: cambio',
      'data: {"id":"evento-1",',
      'data: "recursos":["HOY"]}',
      ''
    ].join('\r\n'))

    expect(eventos).toEqual([{
      id: 'evento-1',
      tipo: 'cambio',
      datos: '{"id":"evento-1",\n"recursos":["HOY"]}'
    }])
  })

  it('ignora bloques sin datos privados', () => {
    expect(extraerEventosSse(': latido\n\n')).toEqual([])
  })

  it('descarta eventos repetidos y limita la memoria de deduplicación', () => {
    const aceptar = crearRegistroEventos(2)
    expect(aceptar('uno')).toBe(true)
    expect(aceptar('uno')).toBe(false)
    expect(aceptar('dos')).toBe(true)
    expect(aceptar('tres')).toBe(true)
    expect(aceptar('uno')).toBe(true)
  })

  it('aplica backoff exponencial con límite de treinta segundos', () => {
    expect(demoraReconexion(1, 0)).toBe(1_500)
    expect(demoraReconexion(2, 0)).toBe(3_000)
    expect(demoraReconexion(6, 0)).toBe(30_000)
    expect(demoraReconexion(20, 0)).toBe(30_000)
  })
})
