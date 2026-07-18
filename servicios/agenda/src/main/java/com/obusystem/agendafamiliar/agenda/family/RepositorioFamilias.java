package com.obusystem.agendafamiliar.agenda.family;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioFamilias extends JpaRepository<Familia, Long> {
    Optional<Familia> findByIdPublico(UUID idPublico);
}
