package com.reminderU.reminderU.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reminderU.reminderU.modelo.*;
import com.reminderU.reminderU.repository.UniversidadRepository;
import com.reminderU.reminderU.service.UniversidadService;
import com.reminderU.reminderU.service.UsuarioService;
import com.reminderU.reminderU.service.impl.CustomOAuth2User;
import com.reminderU.reminderU.service.impl.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/onboarding")
public class OnboardingController {

    private final UniversidadService universidadService;
    private final UniversidadRepository universidadRepository;
    private final UsuarioService usuarioService;
    private final ObjectMapper objectMapper;

    public OnboardingController(UniversidadService universidadService,
                                UniversidadRepository universidadRepository,
                                UsuarioService usuarioService,
                                ObjectMapper objectMapper) {
        this.universidadService    = universidadService;
        this.universidadRepository = universidadRepository;
        this.usuarioService        = usuarioService;
        this.objectMapper          = objectMapper;
    }

    private Usuario obtenerUsuarioActual(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomOAuth2User)  return ((CustomOAuth2User) principal).getUsuario();
        if (principal instanceof CustomUserDetails) return ((CustomUserDetails) principal).getUsuario();
        return usuarioService.obtenerPorEmail(authentication.getName());
    }

    // ─── GET /onboarding ─────────────────────────────────────────────────────
    @GetMapping
    public String mostrarOnboarding(Authentication authentication, Model model) {
        Usuario usuario = obtenerUsuarioActual(authentication);
        if (usuario != null && usuario.isOnboardingCompletado()) return "redirect:/perfil/home";

        model.addAttribute("universidades", universidadService.listarActivas());
        model.addAttribute("usuario", usuario);
        return "onboarding/onboarding";
    }

    // ─── POST /onboarding/guardar (predefinidas) ──────────────────────────────
    @PostMapping("/guardar")
    public String guardarOnboarding(
            Authentication authentication,
            @RequestParam("universidadId") Long universidadId,
            @RequestParam("tipoPeriodo")   String tipoPeriodo,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = obtenerUsuarioActual(authentication);
        if (usuario == null) return "redirect:/login";

        Optional<Universidad> uniOpt = universidadService.buscarPorId(universidadId);
        if (uniOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Universidad no válida");
            return "redirect:/onboarding";
        }

        usuario.setUniversidad(uniOpt.get());
        usuario.setTipoPeriodo(Usuario.TipoPeriodo.valueOf(tipoPeriodo));
        usuario.setOnboardingCompletado(true);
        usuarioService.guardarUsuario(usuario);

        return "redirect:/perfil/home";
    }

    // ─── POST /onboarding/guardar-custom ─────────────────────────────────────
    @PostMapping("/guardar-custom")
    public String guardarOnboardingCustom(
            Authentication authentication,
            @RequestParam("universidadId") Long universidadIdBase,
            @RequestParam("tipoPeriodo")   String tipoPeriodo,
            @RequestParam("dias")          List<String> dias,
            @RequestParam("franjasJson")   String franjasJson,
            RedirectAttributes redirectAttributes) {

        Usuario usuario = obtenerUsuarioActual(authentication);
        if (usuario == null) return "redirect:/login";

        try {
            // 1. Crear universidad CUSTOM propia del usuario
            Universidad uniCustom = new Universidad();
            uniCustom.setNombre("Personalizado - " + usuario.getNombre());
            uniCustom.setSiglas("CUSTOM");
            uniCustom.setCustom(true);
            uniCustom.setActiva(true);

            // 2. Parsear franjas desde JSON enviado por el formulario
            List<Map<String, Object>> franjasData = objectMapper.readValue(
                    franjasJson, new TypeReference<>() {});

            for (Map<String, Object> f : franjasData) {
                HoraFranja franja = new HoraFranja();
                franja.setEtiqueta((String) f.get("etiqueta"));
                franja.setHoraInicio((String) f.get("inicio"));
                franja.setHoraFin((String) f.get("fin"));
                franja.setOrden((int) f.get("orden"));
                franja.setUniversidad(uniCustom);
                uniCustom.getFranjas().add(franja);
            }

            // 3. Agregar días
            for (String diaStr : dias) {
                HorarioDia horarioDia = new HorarioDia();
                horarioDia.setDia(HorarioDia.Dia.valueOf(diaStr));
                horarioDia.setUniversidad(uniCustom);
                uniCustom.getDias().add(horarioDia);
            }

            // 4. Guardar universidad custom en BD
            Universidad uniGuardada = universidadRepository.save(uniCustom);

            // 5. Asignar al usuario y marcar onboarding completo
            usuario.setUniversidad(uniGuardada);
            usuario.setTipoPeriodo(Usuario.TipoPeriodo.valueOf(tipoPeriodo));
            usuario.setOnboardingCompletado(true);
            usuarioService.guardarUsuario(usuario);

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar el horario: " + e.getMessage());
            return "redirect:/onboarding";
        }

        return "redirect:/perfil/home";
    }
}