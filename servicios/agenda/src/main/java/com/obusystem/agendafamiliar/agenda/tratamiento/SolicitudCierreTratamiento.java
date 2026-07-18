package com.obusystem.agendafamiliar.agenda.tratamiento;

import jakarta.validation.constraints.Size;

public record SolicitudCierreTratamiento(@Size(max = 500) String motivo) { }
