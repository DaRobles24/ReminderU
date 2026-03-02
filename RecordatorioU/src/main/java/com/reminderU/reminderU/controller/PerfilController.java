package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final UsuarioService usuarioService;
    private final HomeController homeController;

    public PerfilController(UsuarioService usuarioService, HomeController homeController) {
        this.usuarioService = usuarioService;
        this.homeController = homeController;
    }

    private String obtenerEmailDeAutenticacion(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            return oauth2User.getAttribute("email");
        } else {
            return authentication.getName();
        }
    }

    // ================================
    // HOME/DASHBOARD — redirige a / para que HomeController cargue todo
    // ================================
    @GetMapping("/home")
    public String perfilHome(Authentication authentication, Model model) {
        // Delegamos al HomeController para que cargue tareas, semanas, Q, etc.
        return homeController.index(model, authentication);
    }

    // ================================
    // VER PERFIL
    // ================================
    @GetMapping("/ver")
    public String verMiPerfil(Authentication authentication, Model model) {
        String email = obtenerEmailDeAutenticacion(authentication);
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        if (usuario == null) return "redirect:/login?error=true";
        model.addAttribute("usuario", usuario);
        return "perfil/ver";
    }

    // ================================
    // EDITAR PERFIL
    // ================================
    @GetMapping("/editar")
    public String editarPerfil(Authentication authentication, Model model) {
        String email = obtenerEmailDeAutenticacion(authentication);
        Usuario usuario = usuarioService.obtenerPorEmail(email);
        if (usuario == null) return "redirect:/login?error=true";
        model.addAttribute("usuario", usuario);
        return "perfil/editar";
    }

    // ================================
    // VER PERFIL POR ID
    // ================================
    @GetMapping("/{id}")
    public String verPerfilPorId(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario == null) return "redirect:/perfil/home?error=usuario_no_encontrado";
        model.addAttribute("usuario", usuario);
        return "perfil/ver";
    }

    // ================================
    // GUARDAR PERFIL
    // ================================
    @PostMapping("/editar")
    public String guardarPerfil(
            Authentication authentication,
            @ModelAttribute Usuario usuarioForm,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            Model model) {
        try {
            String email = obtenerEmailDeAutenticacion(authentication);
            Usuario usuarioActual = usuarioService.obtenerPorEmail(email);
            if (usuarioActual == null) return "redirect:/login?error=true";

            usuarioActual.setNombre(usuarioForm.getNombre());
            usuarioActual.setApellido(usuarioForm.getApellido());
            usuarioActual.setCarrera(usuarioForm.getCarrera());

            if (foto != null && !foto.isEmpty()) {
                String uploadDir = "uploads/usuarios/";
                Files.createDirectories(Paths.get(uploadDir));
                String nombreArchivo = usuarioActual.getIdUsuario() + "_" + foto.getOriginalFilename();
                Path filePath = Paths.get(uploadDir, nombreArchivo);
                foto.transferTo(filePath.toFile());
                usuarioActual.setFotoPerfil("/" + uploadDir + nombreArchivo);
            }

            usuarioService.guardarUsuario(usuarioActual);
            return "redirect:/perfil/ver?success=true";
        } catch (IOException e) {
            model.addAttribute("error", "Error al guardar la foto de perfil");
            model.addAttribute("usuario", usuarioForm);
            return "perfil/editar";
        }
    }
}