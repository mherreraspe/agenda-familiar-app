import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Agenda Familiar OBU System',
        short_name: 'Agenda Familiar',
        description: 'Recordatorios familiares privados y sencillos.',
        theme_color: '#315b4c',
        background_color: '#f7f4ec',
        display: 'standalone',
        lang: 'es-PE',
        start_url: '/',
        icons: [
          {
            src: '/icono.svg',
            sizes: 'any',
            type: 'image/svg+xml',
            purpose: 'any maskable'
          }
        ]
      },
      workbox: {
        navigateFallback: '/index.html'
      }
    })
  ],
  server: {
    port: 5173,
    host: '0.0.0.0'
  }
})
