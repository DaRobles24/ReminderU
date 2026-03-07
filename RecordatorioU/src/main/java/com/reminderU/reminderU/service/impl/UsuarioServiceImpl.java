package com.reminderU.reminderU.service.impl;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.repository.CursoRepository;
import com.reminderU.reminderU.repository.TareaRepository;
import com.reminderU.reminderU.repository.UsuarioRepository;
import com.reminderU.reminderU.service.EmailService;
import com.reminderU.reminderU.service.UsuarioService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService, UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final TareaRepository tareaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository,
                              CursoRepository cursoRepository,
                              TareaRepository tareaRepository,
                              PasswordEncoder passwordEncoder,
                              EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.cursoRepository = cursoRepository;
        this.tareaRepository = tareaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    // Guardar sin enviar correo (editar perfil, actualizar datos)
    @Override
    public Usuario guardarUsuario(Usuario usuario) {
        String rawPassword = usuario.getPassword();
        if (rawPassword != null && !rawPassword.startsWith("$2a$") && !rawPassword.startsWith("$2b$")) {
            usuario.setPassword(passwordEncoder.encode(rawPassword));
        }
        return usuarioRepository.save(usuario);
    }

    // Registrar nuevo usuario + enviar correo de bienvenida
    @Override
    public Usuario registrarNuevoUsuario(Usuario usuario) {
        Usuario savedUser = guardarUsuario(usuario);
        try {
            String subject = "¡Bienvenido a ReminderU!";
            String body = "Hola " + savedUser.getNombre() + ",\n\n" +
                    "Gracias por registrarte en ReminderU. Ahora puedes gestionar tus cursos y tareas de manera fácil.\n\n" +
                    "¡Disfruta tu experiencia!\n\n" +
                    "Saludos,\nEl equipo de ReminderU";
            emailService.enviarEmail(savedUser.getEmail(), subject, body);
        } catch (Exception e) {
            System.err.println("Error enviando correo de bienvenida: " + e.getMessage());
        }
        return savedUser;
    }

    // Eliminar cuenta en cascada: tareas → cursos → usuario
    @Override
    @Transactional
    public void eliminarUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) return;

        // 1. Obtener todos los cursos del usuario
        List<Curso> cursos = cursoRepository.findByUsuario(usuario);

        // 2. Eliminar todas las tareas de cada curso
        for (Curso curso : cursos) {
            tareaRepository.deleteByCursoId(curso.getId());
        }

        // 3. Eliminar todos los cursos
        cursoRepository.deleteAll(cursos);

        // 4. Eliminar el usuario
        usuarioRepository.deleteById(id);
    }

    @Override
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    // UserDetailsService
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword())
                .roles("USER")
                .build();
    }
}