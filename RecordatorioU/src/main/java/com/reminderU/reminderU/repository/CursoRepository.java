package com.reminderU.reminderU.repository;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
    
    // Buscar todos los cursos de un usuario específico
    List<Curso> findByUsuario(Usuario usuario);
    
    // Buscar cursos por ID de usuario (alternativa)
    List<Curso> findByUsuarioIdUsuario(Long idUsuario);
}