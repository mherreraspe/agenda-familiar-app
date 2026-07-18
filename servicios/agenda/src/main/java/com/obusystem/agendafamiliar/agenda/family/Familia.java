package com.obusystem.agendafamiliar.agenda.family;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "familias")
public class Familia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "id_publico", nullable = false, unique = true)
    private UUID idPublico;
    @Column(nullable = false)
    private String nombre;
    @Column(name = "zona_horaria", nullable = false)
    private String zonaHoraria;
    @Column(name = "cuota_bytes", nullable = false)
    private long cuotaBytes;

    protected Familia() { }
    public Long getId() { return id; }
    public UUID getIdPublico() { return idPublico; }
    public String getNombre() { return nombre; }
    public String getZonaHoraria() { return zonaHoraria; }
    public long getCuotaBytes() { return cuotaBytes; }
}
