import { afterEach, describe, expect, it, vi } from 'vitest'
import { consultarHoy, crearTarea, iniciarSesion, type RespuestaHoy } from './api'

const respuestaHoy: RespuestaHoy = {
  familiaId: 'familia', familia: 'Familia', zonaHoraria: 'America/Lima', perfiles: [], tareas: []
}

function fijarConexion(enLinea: boolean) {
  Object.defineProperty(navigator, 'onLine', { configurable: true, value: enLinea })
}

afterEach(() => {
  vi.unstubAllGlobals()
  fijarConexion(true)
})

describe('API en modo sin conexión', () => {
  it('bloquea escrituras antes de llamar a la red', async () => {
    fijarConexion(false)
    const fetch = vi.fn()
    vi.stubGlobal('fetch', fetch)

    await expect(crearTarea({ titulo: 'Tarea', descripcion: '', perfilId: 'perfil', fechaLimite: new Date().toISOString() }))
      .rejects.toThrow('Los cambios solo se pueden guardar')
    expect(fetch).not.toHaveBeenCalled()
  })

  it('reutiliza durante la sesión una lectura ya cargada si cae la red', async () => {
    const token = `cabecera.${btoa(JSON.stringify({ sub: 'usuario-a' }))}.firma`
    const fetch = vi.fn()
      .mockResolvedValueOnce(new Response(JSON.stringify({ accessToken: token, expiraEn: '', usuarioId: 'usuario-a', correo: 'a@familia.test' }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(respuestaHoy), { status: 200 }))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
    vi.stubGlobal('fetch', fetch)

    await iniciarSesion('a@familia.test', 'clave-segura')
    expect(await consultarHoy()).toEqual(respuestaHoy)
    fijarConexion(false)
    expect(await consultarHoy()).toEqual(respuestaHoy)
  })
})
