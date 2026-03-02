package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.Tarea;

import java.util.List;
import java.util.Optional;

public interface TareaService {
    List<Tarea> listarTareas();
    Optional<Tarea> buscarPorId(Long id);
    Tarea guardarTarea(Tarea tarea);
    void eliminarTarea(Long id);

    // Nuevo método para tu vista de curso
    List<Tarea> listarTareasPorCurso(Long cursoId);
}
