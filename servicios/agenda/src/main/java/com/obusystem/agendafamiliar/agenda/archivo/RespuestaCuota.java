package com.obusystem.agendafamiliar.agenda.archivo;

public record RespuestaCuota(long cuotaBytes, long usadosBytes, long disponiblesBytes,
        int porcentaje, String nivel) { }
