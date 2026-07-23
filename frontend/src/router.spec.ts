import { beforeEach, describe, expect, it } from 'vitest'
import { router } from './router'

describe('rutas principales', () => {
  beforeEach(async () => {
    await router.push('/hoy')
  })

  it.each([
    ['/hoy', 'hoy'],
    ['/agenda', 'agenda'],
    ['/salud', 'salud'],
    ['/ajustes/familia', 'familia'],
    ['/actividad', 'actividad']
  ])('resuelve %s como una vista real', async (ruta, nombre) => {
    await router.push(ruta)
    expect(router.currentRoute.value.name).toBe(nombre)
  })

  it.each([
    ['/calendario', '/agenda'],
    ['/botiquin', '/salud'],
    ['/tratamientos', '/salud'],
    ['/familia', '/ajustes/familia']
  ])('redirige %s conservando compatibilidad', async (anterior, actual) => {
    await router.push(anterior)
    expect(router.currentRoute.value.path).toBe(actual)
  })

  it('convierte Revisar en el filtro de atención de Hoy', async () => {
    await router.push('/revisar')
    expect(router.currentRoute.value.name).toBe('hoy')
    expect(router.currentRoute.value.query.filtro).toBe('atencion')
  })

  it('expone el prototipo de Objetos únicamente durante desarrollo', async () => {
    await router.push('/prototipo/objetos')
    expect(router.currentRoute.value.name).toBe('objetos-prototipo')
  })
})
