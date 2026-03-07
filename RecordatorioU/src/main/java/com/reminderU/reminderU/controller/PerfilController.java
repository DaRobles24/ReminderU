package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.service.UsuarioService;
import com.reminderU.reminderU.service.impl.CustomOAuth2User;
import com.reminderU.reminderU.service.impl.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private Usuario obtenerUsuarioActual(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUsuario();
        } else if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUsuario();
        }
        return usuarioService.obtenerPorEmail(authentication.getName());
    }

    // ─── HOME ────────────────────────────────────────────────────────────────
    @GetMapping("/home")
    public String perfilHome(Authentication authentication, Model model) {
        Usuario usuario = obtenerUsuarioActual(authentication);

        // ← NUEVO: si no completó el onboarding, redirigir
        if (usuario != null && !usuario.isOnboardingCompletado()) {
            return "redirect:/onboarding";
        }

        return homeController.index(model, authentication);
    }

    // ─── VER PERFIL ──────────────────────────────────────────────────────────
    @GetMapping("/ver")
    public String verMiPerfil(Authentication authentication, Model model) {
        Usuario usuario = obtenerUsuarioActual(authentication);
        if (usuario == null) return "redirect:/login?error=true";
        model.addAttribute("usuario", usuario);
        return "perfil/ver";
    }

    // ─── VER PERFIL POR ID ───────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String verPerfilPorId(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.obtenerPorId(id);
        if (usuario == null) return "redirect:/perfil/home?error=usuario_no_encontrado";
        model.addAttribute("usuario", usuario);
        return "perfil/ver";
    }

    // ─── EDITAR PERFIL ───────────────────────────────────────────────────────
    @GetMapping("/editar")
    public String editarPerfil(Authentication authentication, Model model) {
        Usuario usuario = obtenerUsuarioActual(authentication);
        if (usuario == null) return "redirect:/login?error=true";
        model.addAttribute("usuario", usuario);
        return "perfil/editar";
    }

    // ─── GUARDAR PERFIL ──────────────────────────────────────────────────────
    @PostMapping("/editar")
    public String guardarPerfil(
            Authentication authentication,
            @ModelAttribute Usuario usuarioForm,
            @RequestParam(value = "foto", required = false) MultipartFile foto,
            Model model) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(authentication);
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

    // ─── ELIMINAR CUENTA ─────────────────────────────────────────────────────
    @PostMapping("/eliminar")
    public String eliminarCuenta(Authentication authentication,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        Usuario usuario = obtenerUsuarioActual(authentication);
        if (usuario == null) return "redirect:/login?error=true";

        new SecurityContextLogoutHandler().logout(request, response, authentication);
        usuarioService.eliminarUsuario(usuario.getIdUsuario());

        return "redirect:/login?eliminado=true";
    }
}