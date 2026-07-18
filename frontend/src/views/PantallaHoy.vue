<script setup lang="ts">
import { ref } from 'vue'
import TarjetaPendiente from '../components/TarjetaPendiente.vue'

const mensaje = ref('')
const fechaActual = new Intl.DateTimeFormat('es-PE', {
  weekday: 'long',
  day: 'numeric',
  month: 'long'
}).format(new Date())

function completar(titulo: string) {
  mensaje.value = `${titulo} quedó marcado como hecho.`
}
</script>

<template>
  <div class="aplicacion">
    <header class="cabecera">
      <div>
        <p class="sobretitulo">{{ fechaActual }}</p>
        <h1>Buenos días</h1>
        <p>Esto es lo importante para tu familia.</p>
      </div>
      <button type="button" class="avatar" aria-label="Abrir perfil">MH</button>
    </header>

    <main>
      <section class="resumen" aria-label="Resumen del día">
        <div><strong>3</strong><span>para hoy</span></div>
        <div><strong>1</strong><span>por revisar</span></div>
        <div><strong>2</strong><span>esta semana</span></div>
      </section>

      <p v-if="mensaje" class="confirmacion" role="status">{{ mensaje }}</p>

      <section class="seccion seccion--alerta">
        <div class="titulo-seccion">
          <div>
            <span class="etiqueta">Necesita atención</span>
            <h2>Atrasado</h2>
          </div>
          <a href="/revisar">Ver bandeja</a>
        </div>
        <TarjetaPendiente
          hora="Ayer"
          titulo="Confirmar tratamiento"
          detalle="Una actividad sigue pendiente de revisión."
          tono="atrasado"
          @completar="completar('El tratamiento')"
        />
      </section>

      <section class="seccion">
        <div class="titulo-seccion">
          <div>
            <span class="etiqueta etiqueta--verde">En orden</span>
            <h2>Hoy</h2>
          </div>
          <button class="filtro" type="button">Toda la familia</button>
        </div>
        <TarjetaPendiente
          hora="9:00"
          titulo="Control médico"
          detalle="Cita familiar · Clínica"
          @completar="completar('La cita')"
        />
        <TarjetaPendiente
          hora="18:30"
          titulo="Medicamento de la tarde"
          detalle="Recordatorio privado"
          @completar="completar('El recordatorio')"
        />
      </section>

      <section class="seccion">
        <div class="titulo-seccion">
          <div>
            <span class="etiqueta etiqueta--arena">Próximamente</span>
            <h2>Próximos siete días</h2>
          </div>
        </div>
        <TarjetaPendiente
          hora="Lun 20"
          titulo="Comprar vitaminas"
          detalle="Tarea · Responsable: Marco"
          tono="proximo"
          @completar="completar('La tarea')"
        />
      </section>
    </main>

    <button type="button" class="agregar" aria-label="Agregar cita, tarea, medicamento o tratamiento">
      <span aria-hidden="true">+</span> Agregar
    </button>

    <nav class="navegacion" aria-label="Navegación principal">
      <a class="activo" href="/">Hoy</a>
      <a href="/calendario">Calendario</a>
      <a href="/familia">Familia</a>
      <a href="/revisar">Revisar <span class="contador">1</span></a>
    </nav>
  </div>
</template>
