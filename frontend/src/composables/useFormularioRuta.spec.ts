import { flushPromises, mount } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'
import { useFormularioRuta } from './useFormularioRuta'

const Vista = defineComponent({
  template: '<button id="abrir" @click="abrir($event.currentTarget)">Abrir</button><button id="cerrar" @click="cerrar()">Cerrar</button>',
  setup() {
    const formulario = useFormularioRuta('evento')
    return formulario
  }
})

async function montar(ruta: string, confirmar = vi.fn(async () => true)) {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/', component: Vista }] })
  await router.push(ruta)
  await router.isReady()
  const wrapper = mount(RouterView, { global: { plugins: [router] }, attachTo: document.body })
  await nextTick()
  const vista = wrapper.findComponent(Vista)
  const estado = vista.vm as unknown as ReturnType<typeof useFormularioRuta>
  estado.registrarConfirmacion(confirmar)
  return { router, wrapper, vista, estado }
}

describe('useFormularioRuta', () => {
  it('abre con query y al cerrar vuelve a la ruta anterior', async () => {
    const { router, vista, wrapper } = await montar('/')
    await vista.get('#abrir').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.crear).toBe('evento')
    await vista.get('#cerrar').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.query.crear).toBeUndefined()
    wrapper.unmount()
  })

  it('elimina la query al cerrar una entrada directa', async () => {
    const { router, vista, wrapper } = await montar('/?crear=evento')
    await vista.get('#cerrar').trigger('click')
    await flushPromises()
    expect(router.currentRoute.value.path).toBe('/')
    expect(router.currentRoute.value.query.crear).toBeUndefined()
    wrapper.unmount()
  })

  it('mantiene abierto el formulario modificado si no se confirma el descarte', async () => {
    const confirmar = vi.fn(async () => false)
    const { router, vista, estado, wrapper } = await montar('/', confirmar)
    await vista.get('#abrir').trigger('click')
    await flushPromises()
    ;(estado as unknown as { modificado: boolean }).modificado = true
    await vista.get('#cerrar').trigger('click')
    await flushPromises()
    expect(confirmar).toHaveBeenCalled()
    expect(router.currentRoute.value.query.crear).toBe('evento')
    wrapper.unmount()
  })
})
