package com.obusystem.agendafamiliar.autenticacion.usuario;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "usuarios")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_publico", nullable = false, unique = true)
    private UUID idPublico;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(name = "clave_hash", nullable = false)
    private String claveHash;

    @Column(nullable = false)
    private String estado;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    @Version
    private long version;

    protected Usuario() {
    }

    public Usuario(UUID idPublico, String correo, String claveHash) {
        this.idPublico = idPublico;
        this.correo = correo.toLowerCase();
        this.claveHash = claveHash;
        this.estado = "ACTIVO";
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    public UUID getIdPublico() { return idPublico; }
    public String getCorreo() { return correo; }
    public String getClaveHash() { return claveHash; }
    public String getEstado() { return estado; }
}
