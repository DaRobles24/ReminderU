package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.Usuario;
import java.util.List;

public interface UsuarioService {
    Usuario obtenerPorId(Long id);
    Usuario guardarUsuario(Usuario usuario);
    List<Usuario> obtenerTodos();

    // Método para login
    Usuario obtenerPorEmail(String email);
}
