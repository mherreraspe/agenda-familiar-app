package com.obusystem.agendafamiliar.agenda.family;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;

@Component
public class ContextoFamilia {
    private final EntityManager entityManager;

    public ContextoFamilia(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void activar(Long familiaId) {
        entityManager.createNativeQuery("SELECT set_config('agenda.familia_id', :familiaId, true)")
                .setParameter("familiaId", familiaId.toString())
                .getSingleResult();
    }
}
