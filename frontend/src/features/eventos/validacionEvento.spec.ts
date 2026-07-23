import { describe, expect, it } from 'vitest'
import { validarEvento, type BorradorEvento } from './validacionEvento'

const base: BorradorEvento = {
  perfilId: '', titulo: 'Reunión', tipo: '', lugar: '', direccion: '', notas: '',
  inicioEn: '2026-07-20T10:00', finEn: '', repetir: false,
  frecuencia: 'SEMANAL', intervalo: 1, hasta: ''
}

describe('validarEvento', () => {
  it('exige título e inicio futuro', () => {
    const errores = validarEvento({ ...base, titulo: ' ', inicioEn: '2026-07-19T09:00' }, 'America/Lima', new Date('2026-07-19T15:00:00Z'))
    expect(errores.titulo).toBeTruthy()
    expect(errores.inicioEn).toBeTruthy()
  })

  it('exige que fin y recurrencia sean posteriores al inicio', () => {
    const errores = validarEvento({
      ...base,
      finEn: '2026-07-20T09:00',
      repetir: true,
      hasta: '2026-07-20T10:00'
    }, 'America/Lima', new Date('2026-07-19T15:00:00Z'))
    expect(errores.finEn).toContain('posterior')
    expect(errores.hasta).toContain('posterior')
  })
})
