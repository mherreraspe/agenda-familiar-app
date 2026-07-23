package com.obusystem.agendafamiliar.agenda.sincronizacion;

import java.util.Set;

public record EventoSincronizacion(String id, Set<RecursoSincronizacion> recursos) {
}
