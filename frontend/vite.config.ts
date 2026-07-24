import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      manifest: {
        name: 'Obu Familia',
        short_name: 'Obu Familia',
        description: 'Recordatorios familiares privados y sencillos.',
        theme_color: '#315b4c',
        background_color: '#f7f4ec',
        display: 'standalone',
        lang: 'es-PE',
        start_url: '/',
        icons: [
          {
            src: '/icono-192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'any'
          },
          {
            src: '/icono-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any'
          },
          {
            src: '/icono-maskable-192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'maskable'
          },
          {
            src: '/icono-maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable'
          }
        ]
      },
      workbox: {
        navigateFallback: '/index.html',
        importScripts: ['/push-sw.js']
      }
    })
  ],
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api/v1/autenticacion': 'http://localhost:8101',
      '/api/v1': 'http://localhost:8102'
    }
  }
})
