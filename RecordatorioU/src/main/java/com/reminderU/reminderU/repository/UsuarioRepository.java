package com.reminderU.reminderU.repository;

import com.reminderU.reminderU.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Usuario findByEmail(String email);

    // Trae el usuario con su universidad (sin colecciones, esas se cargan aparte)
    @Query("SELECT u FROM Usuario u LEFT JOIN FETCH u.universidad WHERE u.email = :email")
    Usuario findByEmailConUniversidad(@Param("email") String email);
}