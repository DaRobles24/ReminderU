package com.reminderU.reminderU.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "horario_franja")
public class HoraFranja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "universidad_id", nullable = false)
    private Universidad universidad;

    @Column(nullable = false)
    private String etiqueta; // ej: "8-11", "11-2"

    @Column(name = "hora_inicio", nullable = false)
    private String horaInicio; // ej: "08:00"

    @Column(name = "hora_fin", nullable = false)
    private String horaFin; // ej: "11:00"

    @Column(nullable = false)
    private int orden;

    public HoraFranja() {}

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Universidad getUniversidad() { return universidad; }
    public void setUniversidad(Universidad universidad) { this.universidad = universidad; }

    public String getEtiqueta() { return etiqueta; }
    public void setEtiqueta(String etiqueta) { this.etiqueta = etiqueta; }

    public String getHoraInicio() { return horaInicio; }
    public void setHoraInicio(String horaInicio) { this.horaInicio = horaInicio; }

    public String getHoraFin() { return horaFin; }
    public void setHoraFin(String horaFin) { this.horaFin = horaFin; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
}