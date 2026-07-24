import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CentroNotificaciones from './CentroNotificaciones.vue'
import {
  consultarNotificaciones,
  guardarPreferenciasNotificacion,
  marcarNotificacionLeida,
  marcarTodasNotificacionesLeidas
} from '../api'

vi.mock('../api', () => ({
  consultarNotificaciones: vi.fn(),
  guardarPreferenciasNotificacion: vi.fn(),
  marcarNotificacionLeida: vi.fn(),
  marcarTodasNotificacionesLeidas: vi.fn(),
  registrarDispositivoNotificacion: vi.fn(),
  revocarDispositivoNotificacion: vi.fn()
}))

const preferencias = {
  tareas: true, eventos: true, salud: true, botiquin: true,
  silencioDesde: '22:00', silencioHasta: '07:00'
}

async function montar() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/hoy', name: 'hoy', component: { template: '<p>Hoy</p>' } }]
  })
  await router.push('/hoy')
  await router.isReady()
  const wrapper = mount(CentroNotificaciones, {
    props: { familiaId: 'familia-1' },
    global: { plugins: [router] },
    attachTo: document.body
  })
  await vi.waitFor(() => expect(consultarNotificaciones).toHaveBeenCalled())
  return { wrapper, router }
}

describe('CentroNotificaciones', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    HTMLDialogElement.prototype.showModal = function () { this.setAttribute('open', '') }
    HTMLDialogElement.prototype.close = function () { this.removeAttribute('open') }
    vi.mocked(consultarNotificaciones).mockResolvedValue({
      avisos: [{
        id: 'aviso-1', tipo: 'TAREA', titulo: 'Tarea pendiente', detalle: 'Comprar vendas',
        destino: '/hoy?aviso=tarea-1', creadaEn: new Date().toISOString()
      }],
      sinLeer: 1,
      preferencias,
      dispositivos: [],
      pushDisponible: false,
      clavePublica: ''
    })
    vi.mocked(marcarNotificacionLeida).mockResolvedValue(undefined)
    vi.mocked(marcarTodasNotificacionesLeidas).mockResolvedValue(undefined)
    vi.mocked(guardarPreferenciasNotificacion).mockResolvedValue(preferencias)
  })

  it('separa leer de resolver y navega al elemento relacionado', async () => {
    const { wrapper, router } = await montar()
    await (wrapper.vm as unknown as { abrir: () => Promise<void> }).abrir()

    expect(wrapper.get('dialog').attributes()).toHaveProperty('open')
    expect(wrapper.text()).toContain('Pulso familiar')
    expect(wrapper.text()).toContain('Comprar vendas')
    expect(wrapper.emitted('contador')?.at(-1)).toEqual([1])

    await wrapper.get('.pulso-familiar li > button').trigger('click')
    await vi.waitFor(() => expect(marcarNotificacionLeida).toHaveBeenCalledWith('aviso-1'))
    await vi.waitFor(() => expect(router.currentRoute.value.fullPath).toBe('/hoy?aviso=tarea-1'))
    wrapper.unmount()
  })

  it('explica por qué el push está desactivado sin ocultar la bandeja privada', async () => {
    const { wrapper } = await montar()
    await (wrapper.vm as unknown as { abrir: () => Promise<void> }).abrir()
    await wrapper.get('.preferencias-avisos > summary').trigger('click')

    expect(wrapper.get('.preferencias-avisos__dispositivo button').attributes()).toHaveProperty('disabled')
    expect(wrapper.text()).toContain('La bandeja privada está disponible')
    wrapper.unmount()
  })
})
