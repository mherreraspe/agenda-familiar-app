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
})
