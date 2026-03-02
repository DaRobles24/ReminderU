package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.Evaluacion;
import java.util.List;
import java.util.Optional;

public interface EvaluacionService {
    List<Evaluacion> listarEvaluaciones();
    Optional<Evaluacion> buscarPorId(Long id);
    Evaluacion guardarEvaluacion(Evaluacion evaluacion);
    void eliminarEvaluacion(Long id);

    // Nuevo método
    List<Evaluacion> listarEvaluacionesPorCurso(Long cursoId);
}
