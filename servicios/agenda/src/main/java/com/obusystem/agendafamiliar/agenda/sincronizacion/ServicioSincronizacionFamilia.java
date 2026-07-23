package com.obusystem.agendafamiliar.agenda.sincronizacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.LongFunction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;

@Service
public class ServicioSincronizacionFamilia {
    private static final long SIN_LIMITE_SERVIDOR = 0L;
    private static final Set<RecursoSincronizacion> TODOS = Set.of(
            RecursoSincronizacion.HOY, RecursoSincronizacion.AGENDA, RecursoSincronizacion.SALUD,
            RecursoSincronizacion.OBJETOS);

    private final AccesoFamilia acceso;
    private final LongFunction<SseEmitter> crearEmisor;
    private final ConcurrentHashMap<UUID, CopyOnWriteArraySet<SseEmitter>> conexiones = new ConcurrentHashMap<>();

    @Autowired
    public ServicioSincronizacionFamilia(AccesoFamilia acceso) {
        this(acceso, SseEmitter::new);
    }

    ServicioSincronizacionFamilia(AccesoFamilia acceso, LongFunction<SseEmitter> crearEmisor) {
        this.acceso = acceso;
        this.crearEmisor = crearEmisor;
    }

    @Transactional(readOnly = true)
    public SseEmitter suscribir(UUID familiaId, Jwt jwt) {
        acceso.autorizar(familiaId, jwt);
        SseEmitter emisor = crearEmisor.apply(SIN_LIMITE_SERVIDOR);
        conexiones.computeIfAbsent(familiaId, ignorada -> new CopyOnWriteArraySet<>()).add(emisor);
        Runnable retirar = () -> retirar(familiaId, emisor);
        emisor.onCompletion(retirar);
        emisor.onTimeout(retirar);
        emisor.onError(error -> retirar.run());
        enviar(familiaId, emisor, new EventoSincronizacion(UUID.randomUUID().toString(), TODOS), "sincronizar");
        return emisor;
    }

    public void publicar(UUID familiaId, RecursoSincronizacion... recursos) {
        publicar(familiaId, UUID.randomUUID().toString(), recursos);
    }

    public void publicarIdempotente(UUID familiaId, String claveIdempotencia, RecursoSincronizacion... recursos) {
        String material = familiaId + ":" + claveIdempotencia;
        publicar(familiaId, UUID.nameUUIDFromBytes(material.getBytes(StandardCharsets.UTF_8)).toString(), recursos);
    }

    private void publicar(UUID familiaId, String id, RecursoSincronizacion... recursos) {
        EventoSincronizacion evento = new EventoSincronizacion(id, Set.copyOf(Arrays.asList(recursos)));
        for (SseEmitter emisor : conexiones.getOrDefault(familiaId, new CopyOnWriteArraySet<>())) {
            enviar(familiaId, emisor, evento, "cambio");
        }
    }

    @Scheduled(fixedRate = 25_000)
    void mantenerConexiones() {
        conexiones.forEach((familiaId, emisores) -> emisores.forEach(emisor -> {
            try {
                emisor.send(SseEmitter.event().comment("latido"));
            } catch (IOException | IllegalStateException error) {
                retirar(familiaId, emisor);
            }
        }));
    }

    private void enviar(UUID familiaId, SseEmitter emisor, EventoSincronizacion evento, String nombre) {
        try {
            emisor.send(SseEmitter.event().id(evento.id()).name(nombre).data(evento));
        } catch (IOException | IllegalStateException error) {
            retirar(familiaId, emisor);
        }
    }

    private void retirar(UUID familiaId, SseEmitter emisor) {
        CopyOnWriteArraySet<SseEmitter> emisores = conexiones.get(familiaId);
        if (emisores == null) return;
        emisores.remove(emisor);
        if (emisores.isEmpty()) conexiones.remove(familiaId, emisores);
    }

    int conexionesActivas(UUID familiaId) {
        return conexiones.getOrDefault(familiaId, new CopyOnWriteArraySet<>()).size();
    }
}
