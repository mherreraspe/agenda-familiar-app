import { createRouter, createWebHistory } from 'vue-router'
import PantallaHoy from './views/PantallaHoy.vue'
import PantallaAdmin from './views/PantallaAdmin.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: to => ({ name: 'hoy', query: to.query }) },
    { path: '/hoy', name: 'hoy', component: PantallaHoy, props: { seccion: 'hoy' } },
    { path: '/agenda', name: 'agenda', component: PantallaHoy, props: { seccion: 'agenda' } },
    { path: '/salud', name: 'salud', component: PantallaHoy, props: { seccion: 'salud' } },
    { path: '/objetos', name: 'objetos', component: PantallaHoy, props: { seccion: 'objetos' } },
    { path: '/ajustes/familia', name: 'familia', component: PantallaHoy, props: { seccion: 'familia' } },
    { path: '/actividad', name: 'actividad', component: PantallaHoy, props: { seccion: 'actividad' } },
    { path: '/admin', name: 'admin', component: PantallaAdmin },
    { path: '/calendario', redirect: to => ({ name: 'agenda', query: to.query }) },
    { path: '/revisar', redirect: to => ({ name: 'hoy', query: { ...to.query, filtro: 'atencion' } }) },
    { path: '/botiquin', redirect: to => ({ name: 'salud', query: { ...to.query, seccion: 'botiquin' } }) },
    { path: '/tratamientos', redirect: to => ({ name: 'salud', query: { ...to.query, seccion: 'tratamientos' } }) },
    { path: '/familia', redirect: to => ({ name: 'familia', query: to.query }) }
  ]
})
