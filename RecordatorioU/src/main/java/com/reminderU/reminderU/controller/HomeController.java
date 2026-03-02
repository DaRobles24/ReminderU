package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.ConfiguracionCuatrimestre;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.repository.TareaRepository;
import com.reminderU.reminderU.repository.UsuarioRepository;
import com.reminderU.reminderU.service.ConfiguracionCuatrimestreService;
import com.reminderU.reminderU.service.impl.CustomOAuth2User;
import com.reminderU.reminderU.service.impl.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Controller
public class HomeController {

    private final ConfiguracionCuatrimestreService configuracionCuatrimestreService;

    @Autowired
    private TareaRepository tareaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public HomeController(ConfiguracionCuatrimestreService configuracionCuatrimestreService) {
        this.configuracionCuatrimestreService = configuracionCuatrimestreService;
    }

    // ─── MÉTODO AUXILIAR: obtener usuario ────────────────────────────────────
    private Usuario obtenerUsuarioDesdeAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        try {
            if (principal instanceof CustomUserDetails) {
                return ((CustomUserDetails) principal).getUsuario();
            } else if (principal instanceof CustomOAuth2User) {
                return ((CustomOAuth2User) principal).getUsuario();
            } else {
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

    // ─── MÉTODO AUXILIAR: cargar datos del dashboard ─────────────────────────
    public String cargarDatosDashboard(Model model, Authentication authentication) {
        Usuario usuario = obtenerUsuarioDesdeAuth(authentication);
        model.addAttribute("usuario", usuario);

        LocalDate hoy = LocalDate.now();

        // Tareas del usuario (siempre se cargan)
        List<Tarea> todasTareas = usuario != null
                ? tareaRepository.findByCurso_Usuario(usuario)
                : Collections.emptyList();

        // Tareas próximas (ventana 7 días)
        List<Map<String, Object>> tareasProximas = todasTareas.stream()
                .filter(t -> t.getEstado() != Tarea.Estado.ENTREGADA)
                .filter(t -> t.getFechaEntrega() != null)
                .map(t -> {
                    Map<String, Object> mapa = new HashMap<>();
                    mapa.put("tarea", t);
                    long diasRestantes = ChronoUnit.DAYS.between(hoy, t.getFechaEntrega());
                    mapa.put("diasRestantes", diasRestantes);
                    String color;
                    if (diasRestantes < 0)       color = "text-danger";
                    else if (diasRestantes > 4)   color = "text-success";
                    else if (diasRestantes >= 1)  color = "text-warning";
                    else                          color = "text-danger alerta-urgente";
                    mapa.put("colorTiempo", color);
                    return mapa;
                })
                .filter(m -> {
                    long dias = (Long) m.get("diasRestantes");
                    return dias <= 7 && dias >= 0;
                })
                .toList();

        model.addAttribute("tareasProximas", tareasProximas);

        // ─── Configuración del cuatrimestre POR USUARIO ───────────────────
        ConfiguracionCuatrimestre config = usuario != null
                ? configuracionCuatrimestreService.getConfiguracionPorUsuario(usuario)
                : null;

        // Sin configuración: mostrar aviso en lugar de cuatrimestre inventado
        if (config == null) {
            model.addAttribute("sinConfiguracion", true);
            model.addAttribute("semanas", Collections.emptyList());
            model.addAttribute("fechaInicioCuatrimestre", null);
            model.addAttribute("fechaFinCuatrimestre", null);
            return "index";
        }

        // Con configuración: calcular barra de progreso
        LocalDate fechaInicio = config.getFechaInicio();
        LocalDate fechaFin    = config.getFechaFin();

        long totalSemanas = ChronoUnit.WEEKS.between(fechaInicio, fechaFin) + 1;
        long semanaActual = ChronoUnit.WEEKS.between(fechaInicio, hoy) + 1;
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
                    long semanaTarea = ChronoUnit.WEEKS.between(fechaInicio, t.getFechaEntrega()) + 1;
                    if (semanaTarea == i) tareasSemana.add(t);
                }
            }
            semana.put("tareas", tareasSemana);
            semanas.add(semana);
        }

        model.addAttribute("sinConfiguracion", false);
        model.addAttribute("semanas", semanas);
        model.addAttribute("fechaInicioCuatrimestre", fechaInicio);
        model.addAttribute("fechaFinCuatrimestre", fechaFin);
        return "index";
    }

    // ─── GET / ───────────────────────────────────────────────────────────────
    @GetMapping("/")
    public String index(Model model, Authentication authentication) {
        return cargarDatosDashboard(model, authentication);
    }

    // ─── POST configurar cuatrimestre ─────────────────────────────────────────
    @PostMapping("/configurar-cuatrimestre")
    public String configurarCuatrimestre(
            @RequestParam("inicio") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate inicio,
            @RequestParam("fin") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fin,
            Authentication authentication) {

        Usuario usuario = obtenerUsuarioDesdeAuth(authentication);
        if (usuario == null) return "redirect:/login";

        // Buscar configuración existente del usuario o crear una nueva
        ConfiguracionCuatrimestre c = configuracionCuatrimestreService.getConfiguracionPorUsuario(usuario);
        if (c == null) c = new ConfiguracionCuatrimestre();
        c.setFechaInicio(inicio);
        c.setFechaFin(fin);
        c.setUsuario(usuario);
        configuracionCuatrimestreService.save(c);

        return "redirect:/";
    }
}