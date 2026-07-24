import { mount } from '@vue/test-utils'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it } from 'vitest'
import AppShell from './AppShell.vue'

const Vista = { template: '<p>Vista</p>' }

async function montar(ruta = '/agenda', familia = 'Familia Herrera', props: Record<string, unknown> = {}) {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/hoy', name: 'hoy', component: Vista },
      { path: '/agenda', name: 'agenda', component: Vista },
      { path: '/salud', name: 'salud', component: Vista },
      { path: '/objetos', name: 'objetos', component: Vista },
      { path: '/ajustes/familia', name: 'familia', component: Vista },
      { path: '/actividad', name: 'actividad', component: Vista },
      { path: '/admin', name: 'admin', component: Vista }
    ]
  })
  await router.push(ruta)
  await router.isReady()
  const wrapper = mount(AppShell, {
    props: { titulo: 'Agenda', subtitulo: 'Tareas y eventos', familia, cantidadAtencion: 2, ...props },
    slots: { default: '<p>Contenido del dominio</p>' },
    global: { plugins: [router] },
    attachTo: document.body
  })
  return { wrapper, router }
}

describe('AppShell', () => {
  it('expone cuatro destinos y deriva el estado activo de la ruta', async () => {
    const { wrapper } = await montar()
    expect(wrapper.findAll('nav a')).toHaveLength(4)
    expect(wrapper.get('a[aria-current="page"]').text()).toContain('Agenda')
    expect(wrapper.text()).toContain('Objetos')
    wrapper.unmount()
  })

  it('usa iconos accesibles y oculta el identificador técnico de la familia de prueba', async () => {
    const { wrapper } = await montar('/hoy', 'FAMILIA_TEST')
    expect(wrapper.findAll('.app-shell__navegacion svg')).toHaveLength(4)
    expect(wrapper.text()).toContain('Mi familia')
    expect(wrapper.text()).not.toContain('FAMILIA_TEST')
    wrapper.unmount()
  })

  it('ofrece altas globales sin convertir Añadir en un destino', async () => {
    const { wrapper } = await montar('/hoy')
    await wrapper.get('.boton-anadir').trigger('click')
    await wrapper.get('.menu-desplegable__panel button').trigger('click')
    expect(wrapper.emitted('anadir')?.[0]).toEqual(['evento'])
    expect(wrapper.text()).toContain('Tarea')
    expect(wrapper.text()).not.toContain('Tarea o recordatorio')
    wrapper.unmount()
  })

  it('ejecuta directamente el alta contextual sin abrir el menú global', async () => {
    const { wrapper } = await montar('/objetos', 'Familia Herrera', { etiquetaAnadir: 'Objeto', tipoAnadirDirecto: 'objeto' })
    await wrapper.get('.boton-anadir').trigger('click')
    expect(wrapper.emitted('anadir')?.[0]).toEqual(['objeto'])
    expect(wrapper.find('.menu-desplegable__panel').exists()).toBe(false)
    wrapper.unmount()
  })

  it('limita el alta contextual de Agenda a tareas y eventos', async () => {
    const { wrapper } = await montar('/agenda', 'Familia Herrera', { tiposAnadir: ['tarea', 'evento'] })
    await wrapper.get('.boton-anadir').trigger('click')
    expect(wrapper.findAll('.menu-desplegable__panel button').map(boton => boton.text()))
      .toEqual(['Tarea', 'Evento, cita o salida'])
    wrapper.unmount()
  })

  it('mantiene Familia y Actividad en el menú secundario', async () => {
    const { wrapper } = await montar('/salud')
    await wrapper.get('.menu-desplegable--avatar > button').trigger('click')
    const enlaces = wrapper.findAll('.menu-desplegable--avatar a').map(enlace => enlace.text())
    expect(enlaces).toEqual(['Familia y permisos', 'Actividad'])
    wrapper.unmount()
  })

  it('muestra Administración únicamente al administrador de plataforma', async () => {
    const { wrapper } = await montar('/salud', 'Familia Herrera', { administradorPlataforma: true })
    await wrapper.get('.menu-desplegable--avatar > button').trigger('click')
    expect(wrapper.findAll('.menu-desplegable--avatar a').map(enlace => enlace.text()))
      .toEqual(['Familia y permisos', 'Actividad', 'Administración'])
    wrapper.unmount()
  })

  it('permite cambiar de familia solo cuando la cuenta tiene varias', async () => {
    const familias = [{ id: 'familia-1', nombre: 'Familia Herrera' }, { id: 'familia-2', nombre: 'Familia Huertas' }]
    const { wrapper } = await montar('/hoy', 'Familia Herrera', { familias, familiaActivaId: 'familia-1' })
    await wrapper.get('.menu-desplegable--avatar > button').trigger('click')

    expect(wrapper.get('button[aria-current="true"]').text()).toContain('Familia Herrera')
    const alternativa = wrapper.findAll('.menu-desplegable--avatar .menu-desplegable__panel button')
      .find(boton => boton.text().includes('Familia Huertas'))!
    await alternativa.trigger('click')

    expect(wrapper.emitted('cambiarFamilia')?.[0]).toEqual(['familia-2'])
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

  it('abre los avisos desde una campana con contador accesible', async () => {
    const { wrapper } = await montar('/hoy', 'Familia Herrera', { cantidadNotificaciones: 3 })
    const campana = wrapper.get('.boton-avisos')
    expect(campana.attributes('aria-label')).toContain('3 sin leer')
    expect(campana.text()).toContain('3')
    await campana.trigger('click')
    expect(wrapper.emitted('notificaciones')).toHaveLength(1)
    wrapper.unmount()
  })
})
