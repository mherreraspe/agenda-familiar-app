import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { consultarSugerencias, crearEvento } from '../../api'
import FormularioEvento from './FormularioEvento.vue'

vi.mock('../../api', () => ({
  consultarSugerencias: vi.fn(),
  crearEvento: vi.fn()
}))

const perfiles = [{ id: 'perfil-1', nombre: 'Mamá', tipo: 'ADULTO' as const, color: '#315b4c', relacion: 'Mamá' }]

describe('FormularioEvento', () => {
  beforeEach(() => {
    vi.mocked(crearEvento).mockResolvedValue({ id: 'evento-1' })
    vi.mocked(consultarSugerencias).mockResolvedValue({ sugerencias: [] })
    HTMLDialogElement.prototype.showModal = function () { this.setAttribute('open', '') }
    HTMLDialogElement.prototype.close = function () { this.removeAttribute('open') }
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  function montar() {
    return mount(FormularioEvento, { props: { abierto: true, perfiles, zonaHoraria: 'America/Lima' }, attachTo: document.body })
  }

  it('valida y enfoca el primer campo incorrecto antes del POST', async () => {
    const wrapper = montar()
    await flushPromises()
    await wrapper.get('#evento-inicio').setValue('2020-01-01T10:00')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(crearEvento).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Revisa los campos')
    expect(document.activeElement).toBe(wrapper.get('#evento-titulo').element)
    wrapper.unmount()
  })

  it('conserva el borrador y muestra el error 400 dentro del formulario', async () => {
    vi.mocked(crearEvento).mockRejectedValueOnce(new Error('El evento no cumple el contrato'))
    const wrapper = montar()
    await flushPromises()
    await wrapper.get('#evento-titulo').setValue('Notaría')
    await wrapper.get('#evento-inicio').setValue('2099-07-19T10:00')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('El evento no cumple el contrato')
    expect((wrapper.get('#evento-titulo').element as HTMLInputElement).value).toBe('Notaría')
    wrapper.unmount()
  })

  it('envía fechas convertidas con la zona familiar', async () => {
    const wrapper = montar()
    await flushPromises()
    await wrapper.get('#evento-titulo').setValue('Control')
    await wrapper.get('#evento-inicio').setValue('2099-07-19T10:00')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(crearEvento).toHaveBeenCalledWith(expect.objectContaining({
      titulo: 'Control',
      inicioEn: '2099-07-19T15:00:00.000Z',
      avisar24h: true,
      avisar1h: true
    }))
    expect(wrapper.emitted('guardado')).toHaveLength(1)
    wrapper.unmount()
  })

  it('ofrece cita y salida como tipos visibles y conserva la selección', async () => {
    const wrapper = montar()
    await flushPromises()
    expect(wrapper.get('#evento-tipo').isVisible()).toBe(true)
    expect(wrapper.get('#evento-tipo').findAll('option').map(opcion => opcion.text()))
      .toContain('Cita')
    await wrapper.get('#evento-titulo').setValue('Control dental')
    await wrapper.get('#evento-inicio').setValue('2099-07-19T10:00')
    await wrapper.get('#evento-tipo').setValue('CITA')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(crearEvento).toHaveBeenCalledWith(expect.objectContaining({ tipo: 'CITA' }))
    wrapper.unmount()
  })

  it('consulta desde dos caracteres, espera 275 ms y limita a tres sugerencias', async () => {
    vi.useFakeTimers()
    vi.mocked(consultarSugerencias).mockResolvedValue({
      sugerencias: Array.from({ length: 5 }, (_, indice) => ({
        tipo: 'EVENTO' as const, entidadId: String(indice), titulo: `Control ${indice}`
      }))
    })
    const wrapper = montar()
    await flushPromises()
    await wrapper.get('#evento-titulo').setValue('Co')
    await wrapper.get('#evento-titulo').trigger('input')
    await vi.advanceTimersByTimeAsync(274)
    expect(consultarSugerencias).not.toHaveBeenCalled()
    await vi.advanceTimersByTimeAsync(1)
    await flushPromises()

    expect(consultarSugerencias).toHaveBeenCalledWith('Co')
    expect(wrapper.findAll('.sugerencias button')).toHaveLength(3)
    wrapper.unmount()
  })
})
