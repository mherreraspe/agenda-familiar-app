import { createRouter, createWebHistory } from 'vue-router'
import PantallaHoy from './views/PantallaHoy.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'hoy', component: PantallaHoy },
    { path: '/calendario', name: 'calendario', component: PantallaHoy },
    { path: '/familia', name: 'familia', component: PantallaHoy },
    { path: '/revisar', name: 'revisar', component: PantallaHoy }
  ]
})
