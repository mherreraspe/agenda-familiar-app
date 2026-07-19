package com.obusystem.agendafamiliar.agenda.archivo;

import java.util.UUID;

public record ContenidoArchivo(UUID id, String mimeType, byte[] contenido) { }
