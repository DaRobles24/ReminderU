package com.reminderU.reminderU.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "tarea")
public class Tarea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea")
    private Long id;

    // ===== Curso =====
    @ManyToOne
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    // ===== Título y descripción =====
    @NotBlank(message = "El título no puede estar vacío")
    @Column(nullable = false)
    private String titulo;

    @NotBlank(message = "La descripción no puede estar vacía")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;

    // ===== Porcentaje =====
    @NotNull(message = "El porcentaje es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El porcentaje debe ser al menos 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "El porcentaje no puede superar 100")
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    // ===== Fechas y hora =====
    @NotNull(message = "La fecha de entrega es obligatoria")
    @Column(name = "fecha_entrega", nullable = false)
    private LocalDate fechaEntrega;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "hora_entrega")
    private LocalTime horaEntrega;

    // ===== Modalidad, Estado y Prioridad =====
    @Enumerated(EnumType.STRING)
    private Modalidad modalidad; // INDIVIDUAL, GRUPAL

    @Enumerated(EnumType.STRING)
    @NotNull
    private Estado estado = Estado.PENDIENTE; // PENDIENTE, ENTREGADA, VENCIDA

    @Enumerated(EnumType.STRING)
    private Prioridad prioridad = Prioridad.MEDIA; // ALTA, MEDIA, BAJA

    // ===== Nota Obtenida =====
    @Column(name = "nota_obtenida")
    private Double notaObtenida;

    // ===== Adjuntos (URLs o paths de archivos) =====
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tarea_adjuntos", joinColumns = @JoinColumn(name = "tarea_id"))
    @Column(name = "archivo")
    private List<String> adjuntos;

    // ===== Recordatorios (fechas previas a la entrega) =====
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tarea_recordatorios", joinColumns = @JoinColumn(name = "tarea_id"))
    @Column(name = "fecha_recordatorio")
    private List<LocalDate> recordatorios;

    // ================= Getters y Setters =================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPorcentaje() { return porcentaje; }
    public void setPorcentaje(BigDecimal porcentaje) { this.porcentaje = porcentaje; }

    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDate fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalTime getHoraEntrega() { return horaEntrega; }
    public void setHoraEntrega(LocalTime horaEntrega) { this.horaEntrega = horaEntrega; }

    public Modalidad getModalidad() { return modalidad; }
    public void setModalidad(Modalidad modalidad) { this.modalidad = modalidad; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }

    public Double getNotaObtenida() { return notaObtenida; }
    public void setNotaObtenida(Double notaObtenida) { this.notaObtenida = notaObtenida; }

    public List<String> getAdjuntos() { return adjuntos; }
    public void setAdjuntos(List<String> adjuntos) { this.adjuntos = adjuntos; }

    public List<LocalDate> getRecordatorios() { return recordatorios; }
    public void setRecordatorios(List<LocalDate> recordatorios) { this.recordatorios = recordatorios; }

    // ================= Métodos utilitarios =================
    public boolean venceEnMenosDe(int dias) {
        if (fechaEntrega == null) return false;
        LocalDate hoy = LocalDate.now();
        LocalDate limite = hoy.plusDays(dias);
        return !fechaEntrega.isBefore(hoy) && !fechaEntrega.isAfter(limite);
    }

    public Usuario getUsuario() {
        return curso != null ? curso.getUsuario() : null;
    }

    public String getNombre() {
        return titulo;
    }

    // ================= Enums =================
    public enum Modalidad { INDIVIDUAL, GRUPAL }
    public enum Estado { PENDIENTE, ENTREGADA, VENCIDA }
    public enum Prioridad { ALTA, MEDIA, BAJA }
}
