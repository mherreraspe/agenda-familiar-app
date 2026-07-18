import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import TarjetaPendiente from './TarjetaPendiente.vue'

describe('TarjetaPendiente', () => {
  it('emite la acción completar', async () => {
    const wrapper = mount(TarjetaPendiente, {
      props: { hora: '9:00', titulo: 'Control', detalle: 'Cita familiar' }
    })

    await wrapper.get('button').trigger('click')

    expect(wrapper.emitted('completar')).toHaveLength(1)
  })

  it('ofrece omitir y reprogramar sin ocultar la recurrencia', async () => {
    const wrapper = mount(TarjetaPendiente, {
      props: { hora: '9:00', titulo: 'Control', detalle: 'Cita familiar', recurrente: true }
    })
    const botones = wrapper.findAll('button')
    await botones[1].trigger('click')
    await botones[2].trigger('click')
    expect(wrapper.text()).toContain('Recurrente')
    expect(wrapper.emitted('omitir')).toHaveLength(1)
    expect(wrapper.emitted('reprogramar')).toHaveLength(1)
  })
})
