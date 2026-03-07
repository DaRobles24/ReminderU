package com.reminderU.reminderU.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "horario_dia")
public class HorarioDia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "universidad_id", nullable = false)
    private Universidad universidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Dia dia;

    public enum Dia {
        LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO, DOMINGO
    }

    public HorarioDia() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Universidad getUniversidad() { return universidad; }
    public void setUniversidad(Universidad universidad) { this.universidad = universidad; }

    public Dia getDia() { return dia; }
    public void setDia(Dia dia) { this.dia = dia; }
}