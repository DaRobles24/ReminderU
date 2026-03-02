package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.Usuario;

import java.util.List;
import java.util.Optional;

public interface CursoService {
    
    List<Curso> listarCursos();
    
    // Nuevo: listar cursos por usuario
    List<Curso> listarCursosPorUsuario(Usuario usuario);
    
    Optional<Curso> buscarPorId(Long id);
    
    Curso guardarCurso(Curso curso);
    
    void eliminarCurso(Long id);
}