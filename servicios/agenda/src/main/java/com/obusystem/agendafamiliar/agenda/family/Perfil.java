package com.obusystem.agendafamiliar.agenda.family;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfiles")
public class Perfil {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "id_publico", nullable = false, unique = true)
    private UUID idPublico;
    @Column(name = "familia_id", nullable = false)
    private Long familiaId;
    @Column(name = "nombre_visible", nullable = false)
    private String nombreVisible;
    @Column(nullable = false)
    private String tipo;
    private String color;
    private String relacion;
    @Column(nullable = false)
    private boolean activo;

    protected Perfil() { }
    public Long getId() { return id; }
    public UUID getIdPublico() { return idPublico; }
    public String getNombreVisible() { return nombreVisible; }
    public String getTipo() { return tipo; }
    public String getColor() { return color; }
    public String getRelacion() { return relacion; }
    public boolean isActivo() { return activo; }
}
