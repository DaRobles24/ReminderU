package com.reminderU.reminderU.repository;

import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TareaRepository extends JpaRepository<Tarea, Long> {

    List<Tarea> findByCursoId(Long cursoId);

    List<Tarea> findByCurso_Usuario(Usuario usuario);

    @Transactional
    void deleteByCursoId(Long cursoId);
}