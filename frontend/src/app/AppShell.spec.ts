import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import AppShell from './AppShell.vue'

const Vista = { template: '<p>Vista</p>' }

async function montar(ruta = '/agenda') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/hoy', name: 'hoy', component: Vista },
      { path: '/agenda', name: 'agenda', component: Vista },
      { path: '/salud', name: 'salud', component: Vista },
      { path: '/ajustes/familia', name: 'familia', component: Vista },
      { path: '/actividad', name: 'actividad', component: Vista }
    ]
  })
  await router.push(ruta)
  await router.isReady()
  const wrapper = mount(AppShell, {
    props: { titulo: 'Agenda', subtitulo: 'Tareas y eventos', familia: 'Familia Herrera', cantidadAtencion: 2 },
    slots: { default: '<p>Contenido del dominio</p>' },
    global: { plugins: [router] },
    attachTo: document.body
  })
  return { wrapper, router }
}

describe('AppShell', () => {
  it('expone tres destinos y deriva el estado activo de la ruta', async () => {
    const { wrapper } = await montar()
    expect(wrapper.findAll('nav a')).toHaveLength(3)
    expect(wrapper.get('a[aria-current="page"]').text()).toContain('Agenda')
    expect(wrapper.text()).not.toContain('Objetos')
    wrapper.unmount()
  })

  it('ofrece altas globales sin convertir Añadir en un destino', async () => {
    const { wrapper } = await montar('/hoy')
    await wrapper.get('.boton-anadir').trigger('click')
    await wrapper.get('.menu-desplegable__panel button').trigger('click')
    expect(wrapper.emitted('anadir')?.[0]).toEqual(['evento'])
    wrapper.unmount()
  })

  it('mantiene Familia y Actividad en el menú secundario', async () => {
    const { wrapper } = await montar('/salud')
    await wrapper.get('.menu-desplegable--avatar > button').trigger('click')
    const enlaces = wrapper.findAll('.menu-desplegable--avatar a').map(enlace => enlace.text())
    expect(enlaces).toEqual(['Familia y permisos', 'Actividad'])
    wrapper.unmount()
  })

  it('mantiene un solo menú de cabecera abierto', async () => {
    const { wrapper } = await montar('/salud')
    const menus = wrapper.findAll('.menu-desplegable')
    await menus[0].get('button').trigger('click')
    await menus[1].get('button').trigger('click')

    expect(menus[0].get('button').attributes('aria-expanded')).toBe('false')
    expect(menus[1].get('button').attributes('aria-expanded')).toBe('true')
    expect(wrapper.findAll('.menu-desplegable__panel')).toHaveLength(1)
    wrapper.unmount()
  })
})
