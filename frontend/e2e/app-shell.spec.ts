import { expect, test, type Page } from '@playwright/test'

async function prepararApi(page: Page) {
  const perfil = { id: 'perfil-1', nombre: 'Mamá', tipo: 'ADULTO', color: '#315b4c', relacion: 'Mamá' }
  await page.route('**/api/v1/**', async route => {
    const ruta = new URL(route.request().url()).pathname
    const responder = (datos: unknown) => route.fulfill({ contentType: 'application/json', body: JSON.stringify(datos) })
    if (ruta.endsWith('/autenticacion/renovar')) return responder({ accessToken: 'token-e2e', expiraEn: '2099-01-01T00:00:00Z', usuarioId: 'usuario-1', correo: 'mama@familia.test' })
    if (ruta.endsWith('/hoy')) return responder({ familiaId: 'familia-1', familia: 'Familia Herrera', zonaHoraria: 'America/Lima', perfiles: [perfil], tareas: [] })
    if (ruta.endsWith('/catalogo')) return responder({ medicamentos: [], tratamientos: [], eventos: [], lugares: [] })
    if (ruta.endsWith('/ocurrencias')) return responder({ ocurrencias: [], revisar: [] })
    if (ruta.endsWith('/auditoria')) return responder({ entradas: [] })
    if (ruta.endsWith('/configuracion')) return responder({ puedeAdministrar: true, perfiles: [{ ...perfil, activo: true, permiso: 'ADMINISTRADOR_FAMILIAR' }] })
    if (ruta.endsWith('/archivos/cuota')) return responder({ cuotaBytes: 1_000_000, usadosBytes: 0, disponiblesBytes: 1_000_000, porcentaje: 0, nivel: 'NORMAL' })
    return route.fulfill({ status: 404, contentType: 'application/json', body: '{}' })
  })
}

test.beforeEach(async ({ page }) => prepararApi(page))

test('navega por destinos reales y muestra solo el dominio activo', async ({ page }) => {
  await page.goto('/hoy')
  await expect(page.getByRole('link', { name: /Hoy/ })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('heading', { name: 'Tareas de hoy' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Medicamentos' })).toHaveCount(0)

  await page.getByRole('link', { name: 'Agenda' }).click()
  await expect(page).toHaveURL(/\/agenda$/)
  await expect(page.getByRole('link', { name: 'Agenda' })).toHaveAttribute('aria-current', 'page')
  await expect(page.getByRole('heading', { name: 'Próximos eventos' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Ocurrencias' })).toHaveCount(0)

  await page.getByRole('link', { name: 'Salud' }).click()
  await expect(page).toHaveURL(/\/salud$/)
  await expect(page.getByRole('heading', { name: 'Tratamientos' })).toBeVisible()
  await expect(page.getByRole('heading', { name: 'Próximos eventos' })).toHaveCount(0)
})

test('mantiene Familia y Actividad fuera de la navegación principal', async ({ page }) => {
  await page.goto('/salud')
  await page.locator('summary[aria-label="Abrir menú de familia"]').click()
  await page.getByRole('link', { name: 'Familia y permisos' }).click()
  await expect(page).toHaveURL(/\/ajustes\/familia$/)
  await expect(page.getByRole('heading', { name: 'Perfiles y permisos' })).toBeVisible()
  await expect(page.getByRole('navigation', { name: 'Navegación principal' }).getByText('Objetos')).toHaveCount(0)
})

test('redirige rutas antiguas y evita desbordamiento horizontal', async ({ page }) => {
  await page.goto('/botiquin')
  await expect(page).toHaveURL(/\/salud\?seccion=botiquin$/)
  const medidas = await page.evaluate(() => ({ ancho: document.documentElement.scrollWidth, viewport: window.innerWidth }))
  expect(medidas.ancho).toBeLessThanOrEqual(medidas.viewport)
})
