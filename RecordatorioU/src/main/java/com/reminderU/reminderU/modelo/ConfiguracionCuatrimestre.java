package com.reminderU.reminderU.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "configuracion_cuatrimestre")
public class ConfiguracionCuatrimestre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    // Cada usuario tiene su propia configuración
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    public ConfiguracionCuatrimestre() {}

    public ConfiguracionCuatrimestre(LocalDate inicio, LocalDate fin, Usuario usuario) {
        this.fechaInicio = inicio;
        this.fechaFin = fin;
        this.usuario = usuario;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}