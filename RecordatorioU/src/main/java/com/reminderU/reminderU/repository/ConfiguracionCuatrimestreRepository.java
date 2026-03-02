package com.reminderU.reminderU.repository;

import com.reminderU.reminderU.modelo.ConfiguracionCuatrimestre;
import com.reminderU.reminderU.modelo.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionCuatrimestreRepository extends JpaRepository<ConfiguracionCuatrimestre, Long> {

    // Buscar configuración del cuatrimestre por usuario
    Optional<ConfiguracionCuatrimestre> findTopByUsuarioOrderByIdAsc(Usuario usuario);
}