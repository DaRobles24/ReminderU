package com.reminderU.reminderU.service;

import com.reminderU.reminderU.modelo.Usuario;
import java.util.List;

public interface UsuarioService {
    Usuario obtenerPorId(Long id);
    Usuario guardarUsuario(Usuario usuario);
    Usuario registrarNuevoUsuario(Usuario usuario); // solo para registro nuevo (envía correo)
    void eliminarUsuario(Long id);
    List<Usuario> obtenerTodos();
    Usuario obtenerPorEmail(String email);
}