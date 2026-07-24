self.addEventListener('push', (evento) => {
  evento.waitUntil(self.registration.showNotification('Obu Familia', {
    body: 'Tienes algo por revisar.',
    icon: '/icono-192.png',
    badge: '/icono-192.png',
    tag: 'agenda-familiar-aviso',
    renotify: true,
    data: { destino: '/hoy?avisos=1' }
  }))
})

self.addEventListener('notificationclick', (evento) => {
  evento.notification.close()
  const destino = new URL(evento.notification.data?.destino || '/hoy?avisos=1', self.location.origin).href
  evento.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((ventanas) => {
    const existente = ventanas.find((ventana) => ventana.url.startsWith(self.location.origin))
    if (existente) return existente.navigate(destino).then(() => existente.focus())
    return self.clients.openWindow(destino)
  }))
})
