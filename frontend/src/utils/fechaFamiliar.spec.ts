import { describe, expect, it } from 'vitest'
import { fechaFamiliarAInstant, instantAFechaFamiliar, redondearSiguienteCuarto } from './fechaFamiliar'

describe('fechaFamiliar', () => {
  it('convierte una fecha de la familia sin depender de la zona del dispositivo', () => {
    expect(fechaFamiliarAInstant('2026-07-19T10:30', 'America/Lima')).toBe('2026-07-19T15:30:00.000Z')
    expect(instantAFechaFamiliar('2026-07-19T15:30:00.000Z', 'America/Lima')).toBe('2026-07-19T10:30')
  })

  it('redondea siempre al siguiente cuarto de hora', () => {
    expect(redondearSiguienteCuarto(new Date('2026-07-19T15:07:00.000Z'), 'America/Lima')).toBe('2026-07-19T10:15')
    expect(redondearSiguienteCuarto(new Date('2026-07-19T15:15:00.000Z'), 'America/Lima')).toBe('2026-07-19T10:30')
  })
})
