package com.reminderU.reminderU.modelo;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "universidad")
public class Universidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String siglas;

    @Column(name = "is_custom", nullable = false)
    private boolean isCustom = false;

    @Column(nullable = false)
    private boolean activa = true;

    // EAGER para que se carguen automáticamente junto con la universidad
    @OneToMany(mappedBy = "universidad", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<HoraFranja> franjas = new ArrayList<>();

    @OneToMany(mappedBy = "universidad", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<HorarioDia> dias = new ArrayList<>();

    public Universidad() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getSiglas() { return siglas; }
    public void setSiglas(String siglas) { this.siglas = siglas; }

    public boolean isCustom() { return isCustom; }
    public void setCustom(boolean custom) { isCustom = custom; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public List<HoraFranja> getFranjas() { return franjas; }
    public void setFranjas(List<HoraFranja> franjas) { this.franjas = franjas; }

    public List<HorarioDia> getDias() { return dias; }
    public void setDias(List<HorarioDia> dias) { this.dias = dias; }
}