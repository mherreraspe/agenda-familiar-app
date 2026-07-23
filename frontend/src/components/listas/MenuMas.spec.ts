import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MenuMas from './MenuMas.vue'

describe('MenuMas', () => {
  it('expone acciones secundarias y emite la seleccionada', async () => {
    const wrapper = mount(MenuMas, {
      props: { acciones: [{ id: 'omitir', etiqueta: 'Omitir' }, { id: 'reprogramar', etiqueta: 'Reprogramar' }] }
    })
    await wrapper.findAll('button')[1].trigger('click')
    expect(wrapper.emitted('seleccionar')?.[0]).toEqual(['reprogramar'])
    expect(wrapper.get('details').attributes('open')).toBeUndefined()
  })
})
