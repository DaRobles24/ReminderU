package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.ConfiguracionCuatrimestre;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.repository.TareaRepository;
import com.reminderU.reminderU.repository.UsuarioRepository;
import com.reminderU.reminderU.service.impl.TareaServiceImpl;
import com.reminderU.reminderU.service.ConfiguracionCuatrimestreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class HomeController {

    private final TareaServiceImpl tareaService;
    private final ConfiguracionCuatrimestreService configuracionCuatrimestreService;

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public HomeController(TareaServiceImpl tareaService,
                          ConfiguracionCuatrimestreService configuracionCuatrimestreService) {
        this.tareaService = tareaService;
        this.configuracionCuatrimestreService = configuracionCuatrimestreService;
    }

    @GetMapping("/")
    public String index(Model model, Authentication authentication) {
        // --------- Configuración del cuatrimestre ----------
        ConfiguracionCuatrimestre config = configuracionCuatrimestreService.getConfiguracion();
        LocalDate fechaInicioCuatrimestre = config != null ? config.getFechaInicio() : LocalDate.of(2025, 9, 1);
        LocalDate fechaFinCuatrimestre   = config != null ? config.getFechaFin()    : LocalDate.of(2025, 12, 15);

        LocalDate hoy = LocalDate.now();

        // --------- ✅ FIX BUG 4: Obtener usuario de forma robusta ----------
        Usuario usuario = obtenerUsuarioDesdeAuth(authentication);

        // --------- ✅ FIX BUG 5: Tareas del usuario con ventana de 7 días ----------
        List<Tarea> todasTareas = usuario != null
                ? tareaRepository.findByCurso_Usuario(usuario)
                : Collections.emptyList();

        List<Map<String, Object>> tareasProximas = todasTareas.stream()
                .filter(t -> t.getEstado() != Tarea.Estado.ENTREGADA)
                .filter(t -> t.getFechaEntrega() != null)
                .map(t -> {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("tarea", t);
                    long diasRestantes = ChronoUnit.DAYS.between(hoy, t.getFechaEntrega());
                    mapa.put("diasRestantes", diasRestantes);

                    String color;
                    if (diasRestantes < 0)            color = "text-danger";
                    else if (diasRestantes > 4)        color = "text-success";
                    else if (diasRestantes >= 1)       color = "text-warning";
                    else                               color = "text-danger alerta-urgente";

                    mapa.put("colorTiempo", color);
                    return mapa;
                })
                // ✅ FIX BUG 5: Ventana ampliada a 7 días (antes era <= 3)
                .filter(m -> {
                    long dias = (Long) m.get("diasRestantes");
                    return dias <= 7 && dias >= 0;
                })
                .toList();

        model.addAttribute("tareasProximas", tareasProximas);

        // --------- Progreso del cuatrimestre ----------
        long totalSemanas = ChronoUnit.WEEKS.between(fechaInicioCuatrimestre, fechaFinCuatrimestre) + 1;
        long semanaActual = ChronoUnit.WEEKS.between(fechaInicioCuatrimestre, hoy) + 1;

        if (semanaActual < 1) semanaActual = 1;
        if (semanaActual > totalSemanas) semanaActual = totalSemanas;

        List<Map<String, Object>> semanas = new ArrayList<>();
        for (int i = 1; i <= totalSemanas; i++) {
            Map<String, Object> semana = new HashMap<>();
            semana.put("numero", i);
            semana.put("completada", i < semanaActual);
            semana.put("actual", i == semanaActual);

            List<Tarea> tareasSemana = new ArrayList<>();
            for (Tarea t : todasTareas) {
                if (t.getFechaEntrega() != null) {
                    long semanaTarea = ChronoUnit.WEEKS.between(fechaInicioCuatrimestre, t.getFechaEntrega()) + 1;
                    if (semanaTarea == i) {
                        tareasSemana.add(t);
                    }
                }
            }
            semana.put("tareas", tareasSemana);
            semanas.add(semana);
        }

        model.addAttribute("semanas", semanas);
        model.addAttribute("fechaInicioCuatrimestre", fechaInicioCuatrimestre);
        model.addAttribute("fechaFinCuatrimestre", fechaFinCuatrimestre);

        return "index";
    }

    @PostMapping("/configurar-cuatrimestre")
    public String configurarCuatrimestre(
            @RequestParam("inicio") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
            @RequestParam("fin") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fin) {

        ConfiguracionCuatrimestre c = configuracionCuatrimestreService.getConfiguracion();
        if (c == null) c = new ConfiguracionCuatrimestre();
        c.setFechaInicio(inicio);
        c.setFechaFin(fin);
        configuracionCuatrimestreService.save(c);

        return "redirect:/";
    }

    /**
     * ✅ FIX BUG 4: Obtiene el Usuario desde cualquier tipo de principal
     * (CustomUserDetails, CustomOAuth2User, o email fallback via BD)
     */
    private Usuario obtenerUsuarioDesdeAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;

        Object principal = authentication.getPrincipal();

        try {
            if (principal instanceof com.reminderU.reminderU.service.impl.CustomUserDetails) {
                return ((com.reminderU.reminderU.service.impl.CustomUserDetails) principal).getUsuario();
            } else if (principal instanceof com.reminderU.reminderU.service.impl.CustomOAuth2User) {
                return ((com.reminderU.reminderU.service.impl.CustomOAuth2User) principal).getUsuario();
            } else {
                // Fallback: buscar por email/username
                String email = authentication.getName();
                if (email != null && !email.isBlank()) {
                    return usuarioRepository.findByEmail(email);
                }
            }
        } catch (Exception ex) {
            System.err.println("HomeController: no se pudo obtener usuario: " + ex.getMessage());
        }
        return null;
    }
}