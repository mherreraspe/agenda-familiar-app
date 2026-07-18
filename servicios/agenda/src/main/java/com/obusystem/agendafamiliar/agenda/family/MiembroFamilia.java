package com.obusystem.agendafamiliar.agenda.family;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "miembros_familia")
public class MiembroFamilia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "familia_id", nullable = false)
    private Long familiaId;
    @Column(name = "usuario_publico_id", nullable = false)
    private UUID usuarioPublicoId;
    @Column(nullable = false)
    private String rol;
    @Column(nullable = false)
    private boolean activo;

    protected MiembroFamilia() { }
}
