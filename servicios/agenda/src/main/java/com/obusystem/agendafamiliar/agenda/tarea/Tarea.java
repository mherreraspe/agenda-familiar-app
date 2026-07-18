package com.obusystem.agendafamiliar.agenda.tarea;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "tareas")
public class Tarea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "id_publico", nullable = false, unique = true)
    private UUID idPublico;
    @Column(name = "familia_id", nullable = false)
    private Long familiaId;
    @Column(name = "perfil_id")
    private Long perfilId;
    @Column(nullable = false)
    private String titulo;
    private String descripcion;
    @Column(name = "fecha_limite", nullable = false)
    private Instant fechaLimite;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoTarea estado;
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;
    @Column(name = "recurrencia_id")
    private Long recurrenciaId;
    @Column(name = "numero_ocurrencia")
    private Integer numeroOcurrencia;
    @Column(name = "tarea_origen_id")
    private Long tareaOrigenId;
    @Version
    private long version;

    protected Tarea() { }

    public Tarea(UUID idPublico, Long familiaId, Long perfilId, String titulo, String descripcion, Instant fechaLimite) {
        this(idPublico, familiaId, perfilId, titulo, descripcion, fechaLimite, null, null);
    }

    public Tarea(UUID idPublico, Long familiaId, Long perfilId, String titulo, String descripcion, Instant fechaLimite,
            Long recurrenciaId, Integer numeroOcurrencia) {
        this.idPublico = idPublico;
        this.familiaId = familiaId;
        this.perfilId = perfilId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.fechaLimite = fechaLimite;
        this.recurrenciaId = recurrenciaId;
        this.numeroOcurrencia = numeroOcurrencia;
        this.estado = EstadoTarea.PENDIENTE;
        this.creadoEn = Instant.now();
        this.actualizadoEn = this.creadoEn;
    }

    public void cambiarEstado(EstadoTarea nuevoEstado) {
        this.estado = nuevoEstado;
        this.actualizadoEn = Instant.now();
    }

    public UUID getIdPublico() { return idPublico; }
    public Long getPerfilId() { return perfilId; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public Instant getFechaLimite() { return fechaLimite; }
    public EstadoTarea getEstado() { return estado; }
    public Long getRecurrenciaId() { return recurrenciaId; }
    public Long getTareaOrigenId() { return tareaOrigenId; }
}
