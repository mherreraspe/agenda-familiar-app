package com.obusystem.agendafamiliar.agenda.tarea;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioTareas extends JpaRepository<Tarea, Long> {
    List<Tarea> findByFamiliaIdAndFechaLimiteBetweenOrderByFechaLimite(Long familiaId, Instant desde, Instant hasta);
    Optional<Tarea> findByFamiliaIdAndIdPublico(Long familiaId, UUID idPublico);
}
