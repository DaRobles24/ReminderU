package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.Rubro;
import java.util.List;
import java.util.Optional;

public interface RubroService {
    List<Rubro> listarRubros();
    Optional<Rubro> buscarPorId(Long id);
    Rubro guardarRubro(Rubro rubro);
    void eliminarRubro(Long id);
}
