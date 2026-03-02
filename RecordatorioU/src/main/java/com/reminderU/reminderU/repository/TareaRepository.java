package com.reminderU.reminderU.repository;

import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    // Listar todas las tareas de un curso específico
    List<Tarea> findByCursoId(Long cursoId);

    // Listar todas las tareas que pertenecen a los cursos de un usuario específico
    List<Tarea> findByCurso_Usuario(Usuario usuario);
}
