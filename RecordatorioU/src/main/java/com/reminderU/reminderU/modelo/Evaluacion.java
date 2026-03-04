package com.reminderU.reminderU.modelo;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "evaluaciones")
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private double porcentaje; // % de la nota total

    private LocalDate fecha;

    @Column(name = "nota_obtenida")
    private Double notaObtenida; // 0–100, null = aún sin nota

    @ManyToOne
    @JoinColumn(name = "curso_id")
    private Curso curso;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public double getPorcentaje() { return porcentaje; }
    public void setPorcentaje(double porcentaje) { this.porcentaje = porcentaje; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public Double getNotaObtenida() { return notaObtenida; }
    public void setNotaObtenida(Double notaObtenida) { this.notaObtenida = notaObtenida; }

    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }
}