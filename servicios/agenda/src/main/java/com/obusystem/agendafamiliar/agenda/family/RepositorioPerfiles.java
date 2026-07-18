package com.obusystem.agendafamiliar.agenda.family;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioPerfiles extends JpaRepository<Perfil, Long> {
    List<Perfil> findByFamiliaIdOrderByNombreVisible(Long familiaId);
    List<Perfil> findByFamiliaIdAndActivoTrueOrderByNombreVisible(Long familiaId);
    Optional<Perfil> findByFamiliaIdAndIdPublico(Long familiaId, UUID idPublico);
}
