package com.obusystem.agendafamiliar.agenda.family;

import java.util.UUID;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioMiembrosFamilia extends JpaRepository<MiembroFamilia, Long> {
    boolean existsByFamiliaIdAndUsuarioPublicoIdAndActivoTrue(Long familiaId, UUID usuarioPublicoId);
    Optional<MiembroFamilia> findByFamiliaIdAndUsuarioPublicoIdAndActivoTrue(Long familiaId, UUID usuarioPublicoId);
}
