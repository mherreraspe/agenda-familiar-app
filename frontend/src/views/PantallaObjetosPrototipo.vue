<script setup lang="ts">
import { computed, ref } from 'vue'
import AppShell from '../app/AppShell.vue'

const busqueda = ref('')
const objetos = [
  { id: 1, nombre: 'Partida notarial', categoria: 'Documentos', ruta: ['Habitación principal', 'Ropero', 'Caja de documentos'] },
  { id: 2, nombre: 'Bandera del Perú', categoria: 'Celebraciones', ruta: ['Cuarto de estudio', 'Baúl grande'] },
  { id: 3, nombre: 'Mochilas de viaje', categoria: 'Viajes', ruta: ['Cochera', 'Estante superior'] }
]

const objetosVisibles = computed(() => {
  const termino = busqueda.value.trim().toLocaleLowerCase('es')
  if (!termino) return objetos
  return objetos.filter(objeto => `${objeto.nombre} ${objeto.categoria} ${objeto.ruta.join(' ')}`.toLocaleLowerCase('es').includes(termino))
})
</script>

<template>
  <AppShell titulo="Objetos" subtitulo="Encuentra lo que guardó tu familia" familia="Mi familia" :mostrar-anadir="false" mostrar-objetos>
    <section class="objetos-prototipo" aria-labelledby="titulo-busqueda-objetos">
      <p class="prototipo-aviso">Prototipo visual · todavía no guarda información</p>
      <div class="buscador-objetos">
        <label id="titulo-busqueda-objetos" for="buscar-objetos">¿Qué estás buscando?</label>
        <input id="buscar-objetos" v-model="busqueda" type="search" placeholder="Objeto, nota o lugar" autocomplete="off" />
      </div>

      <div class="titulo-seccion titulo-seccion--objetos">
        <div><span class="etiqueta etiqueta--verde">A mano</span><h2>Guardados recientemente</h2></div>
        <button type="button" class="boton-secundario" disabled>+ Guardar</button>
      </div>

      <div class="lista-objetos">
        <article v-for="objeto in objetosVisibles" :key="objeto.id" class="fila-objeto">
          <span class="fila-objeto__marca" aria-hidden="true"></span>
          <div><h3>{{ objeto.nombre }}</h3><p>{{ objeto.ruta.join(' › ') }}</p><small>{{ objeto.categoria }}</small></div>
          <span aria-hidden="true">›</span>
        </article>
        <p v-if="!objetosVisibles.length" class="estado-vacio">No encontramos nada con ese nombre o lugar.</p>
      </div>

      <section class="lugares-objetos" aria-labelledby="titulo-lugares">
        <div class="titulo-seccion"><div><span class="etiqueta etiqueta--arena">Por lugar</span><h2 id="titulo-lugares">Explorar la casa</h2></div></div>
        <div class="lugares-objetos__ruta">
          <strong>Casa</strong>
          <span>Habitación principal</span>
          <span>Cuarto de estudio</span>
          <span>Cochera</span>
          <span>Lavandería</span>
        </div>
      </section>
    </section>
  </AppShell>
</template>
