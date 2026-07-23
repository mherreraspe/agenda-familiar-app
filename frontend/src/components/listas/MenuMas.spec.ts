import { mount } from '@vue/test-utils'
import { defineComponent } from 'vue'
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

  it('mantiene un solo menú abierto y cierra con Escape o al pulsar fuera', async () => {
    const Contenedor = defineComponent({
      components: { MenuMas },
      template: '<div id="fuera">Fuera</div><MenuMas :acciones="acciones" etiqueta="Acciones uno" /><MenuMas :acciones="acciones" etiqueta="Acciones dos" />',
      setup: () => ({ acciones: [{ id: 'editar', etiqueta: 'Editar' }] })
    })
    const wrapper = mount(Contenedor, { attachTo: document.body })
    const menus = wrapper.findAll('details')

    await menus[0].get('summary').trigger('click')
    expect(menus[0].attributes('open')).toBeDefined()
    await menus[1].get('summary').trigger('click')
    expect(menus[0].attributes('open')).toBeUndefined()
    expect(menus[1].attributes('open')).toBeDefined()

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await wrapper.vm.$nextTick()
    expect(menus[1].attributes('open')).toBeUndefined()

    await menus[0].get('summary').trigger('click')
    wrapper.get('#fuera').element.dispatchEvent(new Event('pointerdown', { bubbles: true }))
    await wrapper.vm.$nextTick()
    expect(menus[0].attributes('open')).toBeUndefined()
    wrapper.unmount()
  })
})
