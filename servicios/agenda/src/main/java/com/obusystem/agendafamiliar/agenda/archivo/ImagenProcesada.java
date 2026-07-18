package com.obusystem.agendafamiliar.agenda.archivo;

public record ImagenProcesada(byte[] original, byte[] miniatura, int ancho, int alto, String sha256) { }
