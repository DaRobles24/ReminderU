package com.reminderU.reminderU.service.impl;

import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.repository.UsuarioRepository;
import com.reminderU.reminderU.service.EmailService;
import com.reminderU.reminderU.service.UsuarioService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioServiceImpl implements UsuarioService, UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // ✅ Inyectamos EmailService

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, 
                              PasswordEncoder passwordEncoder,
                              EmailService emailService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    public Usuario obtenerPorId(Long id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    @Override
    public Usuario guardarUsuario(Usuario usuario) {
        // ✅ Encriptar password si no lo está
        String rawPassword = usuario.getPassword();
        if (rawPassword != null && !rawPassword.startsWith("$2a$") && !rawPassword.startsWith("$2b$")) {
            usuario.setPassword(passwordEncoder.encode(rawPassword));
        }

        Usuario savedUser = usuarioRepository.save(usuario);

        // ✅ Enviar correo de bienvenida
        String subject = "¡Bienvenido a ReminderU!";
        String body = "Hola " + savedUser.getNombre() + ",\n\n" +
                      "Gracias por registrarte en ReminderU. Ahora puedes gestionar tus cursos y tareas de manera fácil.\n\n" +
                      "¡Disfruta tu experiencia!\n\n" +
                      "Saludos,\nEl equipo de ReminderU";

        emailService.enviarEmail(savedUser.getEmail(), subject, body);

        return savedUser;
    }

    @Override
    public List<Usuario> obtenerTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    // ================= UserDetailsService =================
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPassword()) // ya está encriptado en BD
                .roles("USER") // todos los usuarios tendrán rol USER por ahora
                .build();
    }
}
