package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    public LoginController(UsuarioService usuarioService, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    // ===============================
    // LOGIN
    // ===============================
    @GetMapping("/login")
    public String loginPage(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "registroExitoso", required = false) String registroExitoso,
            @RequestParam(value = "error", required = false) String error,
            Model model) {

        if (user != null) {
            return "redirect:/perfil/home";  // ✅ Esto solo redirige, no mapea la ruta
        }

        if (registroExitoso != null) {
            model.addAttribute("registroExitoso", true);
        }

        if (error != null) {
            model.addAttribute("errorLogin", "Correo o contraseña incorrectos");
        }

        return "login";
    }

    // ===============================
    // REGISTRO
    // ===============================
    @GetMapping("/registro")
    public String registroPage(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(
            @Valid @ModelAttribute("usuario") Usuario usuario,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "registro";
        }

        // Validar email duplicado
        boolean emailExistente = usuarioService.obtenerTodos().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(usuario.getEmail()));
        if (emailExistente) {
            model.addAttribute("errorEmail", "El correo ya está en uso");
            return "registro";
        }

        // Validar cédula duplicada
        boolean cedulaExistente = usuarioService.obtenerTodos().stream()
                .anyMatch(u -> u.getCedula() != null && u.getCedula().equalsIgnoreCase(usuario.getCedula()));
        if (cedulaExistente) {
            model.addAttribute("errorCedula", "La cédula ya está en uso");
            return "registro";
        }

        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            model.addAttribute("errorPassword", "La contraseña no puede estar vacía");
            return "registro";
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword().trim()));
        usuario.setRol("USER");
        usuario.setProveedor("LOCAL");

        usuarioService.guardarUsuario(usuario);

        return "redirect:/login?registroExitoso";
    }

    // ===============================
    // LOGOUT
    // ===============================
    @GetMapping("/logout-success")
    public String logoutPage() {
        return "redirect:/login?logout";
    }
}