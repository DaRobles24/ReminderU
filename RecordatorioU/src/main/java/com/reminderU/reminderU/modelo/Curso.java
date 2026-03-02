package com.reminderU.reminderU.modelo;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "curso")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Column(name = "nombre", nullable = false)
    private String nombre;

    @NotNull(message = "La modalidad es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad", nullable = false)
    private Modalidad modalidad;

    @Column(name = "sede")
    private String sede;

    @NotBlank(message = "El profesor es obligatorio")
    @Column(name = "profesor", nullable = false)
    private String profesor;

    @Email(message = "El correo no es válido")
    @Column(name = "correo")
    private String correo;

    @NotNull(message = "El día es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "dia", nullable = false)
    private Dia dia;

    @NotBlank(message = "La hora es obligatoria")
    @Column(name = "hora", nullable = false)
    private String hora;

    // ===== Relación con Usuario (NUEVO) =====
    @ManyToOne
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // ===== Relación con Rubros =====
    @OneToMany(mappedBy = "curso", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Rubro> rubros = new ArrayList<>();

    // ===== Enums =====
    public enum Modalidad { VIRTUAL, PRESENCIAL }
    public enum Dia { LUNES, MARTES, MIERCOLES, JUEVES, VIERNES, SABADO }

    // ===== Constructores =====
    public Curso() {}

    public Curso(String nombre, Modalidad modalidad, String sede,
                 String profesor, String correo, Dia dia, String hora) {
        this.nombre = nombre;
        this.modalidad = modalidad;
        this.sede = sede;
        this.profesor = profesor;
        this.correo = correo;
        this.dia = dia;
        this.hora = hora;
    }

    // ===== Getters y Setters =====
    public Long getId() { 
        return id; 
    }
    
    public void setId(Long id) { 
        this.id = id; 
    }

    public String getNombre() { 
        return nombre; 
    }
    
    public void setNombre(String nombre) { 
        this.nombre = nombre; 
    }

    public Modalidad getModalidad() { 
        return modalidad; 
    }
    
    public void setModalidad(Modalidad modalidad) { 
        this.modalidad = modalidad; 
    }

    public String getSede() { 
        return sede; 
    }
    
    public void setSede(String sede) { 
        this.sede = sede; 
    }

    public String getProfesor() { 
        return profesor; 
    }
    
    public void setProfesor(String profesor) { 
        this.profesor = profesor; 
    }

    public String getCorreo() { 
        return correo; 
    }
    
    public void setCorreo(String correo) { 
        this.correo = correo; 
    }

    public Dia getDia() { 
        return dia; 
    }
    
    public void setDia(Dia dia) { 
        this.dia = dia; 
    }

    public String getHora() { 
        return hora; 
    }
    
    public void setHora(String hora) { 
        this.hora = hora; 
    }

    public Usuario getUsuario() { 
        return usuario; 
    }
    
    public void setUsuario(Usuario usuario) { 
        this.usuario = usuario; 
    }

    public List<Rubro> getRubros() { 
        return rubros; 
    }
    
    public void setRubros(List<Rubro> rubros) { 
        this.rubros = rubros; 
    }

    // ===== Transient =====
    @Transient
    public String getHorario() {
        return dia.name() + " " + hora;
    }

    // ===== Métodos auxiliares =====
    public void agregarRubro(Rubro rubro) {
        rubros.add(rubro);
        rubro.setCurso(this);
    }

    public void removerRubro(Rubro rubro) {
        rubros.remove(rubro);
        rubro.setCurso(null);
    }
}