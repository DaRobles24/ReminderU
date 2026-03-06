package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.Evaluacion;
import com.reminderU.reminderU.modelo.Rubro;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.service.impl.CursoServiceImpl;
import com.reminderU.reminderU.service.impl.TareaServiceImpl;
import com.reminderU.reminderU.service.impl.EvaluacionServiceImpl;
import com.reminderU.reminderU.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/cursos")
public class CursoController {

    private final CursoServiceImpl cursoService;
    private final TareaServiceImpl tareaService;
    private final EvaluacionServiceImpl evaluacionService;
    private final UsuarioService usuarioService;

    private final List<String> franjasHorarias = List.of("8-11", "11-2", "2-5", "6-9");
    private final List<String> dias = List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO");

    public CursoController(CursoServiceImpl cursoService, TareaServiceImpl tareaService,
                          EvaluacionServiceImpl evaluacionService, UsuarioService usuarioService) {
        this.cursoService = cursoService;
        this.tareaService = tareaService;
        this.evaluacionService = evaluacionService;
        this.usuarioService = usuarioService;
    }

    // Método auxiliar para obtener email
    private String obtenerEmailDeAutenticacion(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            return oauth2User.getAttribute("email");
        } else {
            return authentication.getName();
        }
    }

    // ----------------- LISTAR CURSOS -----------------
    @GetMapping
    public String listarCursos(Authentication authentication, Model model) {
        String email = obtenerEmailDeAutenticacion(authentication);
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        if (usuario == null) {
            return "redirect:/login";
        }

        List<Curso> cursos = cursoService.listarCursosPorUsuario(usuario);
        model.addAttribute("cursosExistentes", cursos);

        Map<String, Map<String, Curso>> mapaCursos = new HashMap<>();
        for (String dia : dias) {
            mapaCursos.put(dia, new HashMap<>());
            for (String hora : franjasHorarias) {
                mapaCursos.get(dia).put(hora, null);
            }
        }
        for (Curso curso : cursos) {
            if (curso.getDia() != null && curso.getHora() != null) {
                String diaKey = curso.getDia().name();
                String horaKey = curso.getHora().trim();
                if (mapaCursos.containsKey(diaKey) && mapaCursos.get(diaKey).containsKey(horaKey)) {
                    mapaCursos.get(diaKey).put(horaKey, curso);
                }
            }
        }

        model.addAttribute("mapaCursos", mapaCursos);
        model.addAttribute("dias", dias);
        model.addAttribute("franjasHorarias", franjasHorarias);

        return "Cursos/curso";
    }

    // ----------------- VER CURSO -----------------
    @GetMapping("/ver/{id}")
    public String verCurso(@PathVariable Long id, Authentication authentication,
                          Model model, RedirectAttributes redirectAttributes) {
        String email = obtenerEmailDeAutenticacion(authentication);
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        Optional<Curso> cursoOpt = cursoService.buscarPorId(id);
        if (cursoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensaje", "Curso no encontrado");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/cursos";
        }

        Curso curso = cursoOpt.get();

        if (!curso.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            redirectAttributes.addFlashAttribute("mensaje", "No tienes permiso para ver este curso");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/cursos";
        }

        model.addAttribute("curso", curso);

        // Tareas
        List<Tarea> todasTareas = tareaService.listarTareasPorCurso(id);
        List<Tarea> tareasPendientes = new ArrayList<>();
        List<Tarea> tareasEntregadas = new ArrayList<>();
        for (Tarea t : todasTareas) {
            if (t.getEstado() == Tarea.Estado.ENTREGADA) {
                tareasEntregadas.add(t);
            } else {
                tareasPendientes.add(t);
            }
        }
        model.addAttribute("tareasPendientes", tareasPendientes);
        model.addAttribute("tareasEntregadas", tareasEntregadas);

        // ── Hora formateada (servidor, sin depender de JS) ────────────────────
        model.addAttribute("horaFormateada", formatearHora(curso.getHora()));

        // Evaluaciones
        List<Evaluacion> desgloseEvaluaciones = evaluacionService.listarEvaluacionesPorCurso(id);
        model.addAttribute("desgloseEvaluaciones", desgloseEvaluaciones);

        // ── Cálculo de nota acumulada ──────────────────────────────────────────
        // Prioridad 1: evaluaciones con notaObtenida registrada
        // Cada evaluación tiene su propio porcentaje (double) sobre el total del curso
        double puntosGanados = 0.0;
        boolean hayNotas = false;

        List<Evaluacion> evaluacionesConNota = desgloseEvaluaciones.stream()
                .filter(e -> e.getNotaObtenida() != null)
                .toList();

        if (!evaluacionesConNota.isEmpty()) {
            for (Evaluacion ev : evaluacionesConNota) {
                // notaObtenida es 0-100, porcentaje es el peso de esta evaluación en el curso
                puntosGanados += ev.getNotaObtenida() * (ev.getPorcentaje() / 100.0);
            }
            hayNotas = true;
        } else {
            // Fallback: tareas entregadas con nota (porcentaje es BigDecimal)
            for (Tarea t : tareasEntregadas) {
                if (t.getNotaObtenida() != null && t.getPorcentaje() != null) {
                    puntosGanados += t.getNotaObtenida() * (t.getPorcentaje().doubleValue() / 100.0);
                    hayNotas = true;
                }
            }
        }

        if (hayNotas) {
            double notaPromedio   = Math.min(puntosGanados, 100.0);
            double puntosParaMeta = Math.max(0.0, 70.0 - notaPromedio);
            double barraProgreso  = Math.min(notaPromedio, 100.0);
            boolean aprobando     = notaPromedio >= 70.0;

            model.addAttribute("notaPromedio",   notaPromedio);
            model.addAttribute("puntosParaMeta", puntosParaMeta);
            model.addAttribute("barraProgreso",  barraProgreso);
            model.addAttribute("aprobando",      aprobando);
            model.addAttribute("hayNotas",       true);
        }
        // Si no hay notas, notaPromedio permanece null y el panel de progreso se oculta
        // ──────────────────────────────────────────────────────────────────────────

        return "Cursos/verCursos";
    }

    // ----------------- FORMULARIO CURSO -----------------
    @GetMapping("/form")
    public String verFormCurso(@RequestParam(value = "id", required = false) Long id,
                              Authentication authentication, Model model) {
        String email = obtenerEmailDeAutenticacion(authentication);
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        Curso curso = (id != null) ? cursoService.buscarPorId(id).orElse(new Curso()) : new Curso();
        while (curso.getRubros().size() < 5) {
            curso.agregarRubro(new Rubro());
        }

        model.addAttribute("curso", curso);
        model.addAttribute("cursosExistentes", cursoService.listarCursosPorUsuario(usuario));
        model.addAttribute("franjasHorarias", franjasHorarias);
        model.addAttribute("dias", dias);

        return "Cursos/formCurso";
    }

    // ----------------- GUARDAR CURSO -----------------
    @PostMapping("/guardar")
    public String guardarCurso(@ModelAttribute Curso curso, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            String email = obtenerEmailDeAutenticacion(authentication);
            Usuario usuario = usuarioService.obtenerPorEmail(email);

            if (usuario == null) {
                return "redirect:/login";
            }

            if (curso.getHora() != null) {
                curso.setHora(curso.getHora().trim());
            }

            Optional<Curso> existente = cursoService.listarCursosPorUsuario(usuario).stream()
                    .filter(c -> c.getDia().equals(curso.getDia())
                    && c.getHora().equals(curso.getHora())
                    && (curso.getId() == null || !c.getId().equals(curso.getId())))
                    .findFirst();

            if (existente.isPresent()) {
                redirectAttributes.addFlashAttribute("mensaje", "¡Ya existe un curso en este horario!");
                redirectAttributes.addFlashAttribute("tipo", "danger");
                redirectAttributes.addFlashAttribute("curso", curso);
                return "redirect:/cursos/form";
            }

            curso.setUsuario(usuario);
            cursoService.guardarCurso(curso);

            redirectAttributes.addFlashAttribute("cursoAgregado", curso);
            redirectAttributes.addFlashAttribute("mensaje", "Curso guardado correctamente");
            redirectAttributes.addFlashAttribute("tipo", "success");

            return "redirect:/cursos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al guardar el curso: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "danger");
            redirectAttributes.addFlashAttribute("curso", curso);
            return "redirect:/cursos/form";
        }
    }

    // ----------------- EDITAR CURSO -----------------
    @GetMapping("/editar/{id}")
    public String editarCurso(@PathVariable Long id, Authentication authentication,
                             Model model, RedirectAttributes redirectAttributes) {
        String email = obtenerEmailDeAutenticacion(authentication);
        Usuario usuario = usuarioService.obtenerPorEmail(email);

        Optional<Curso> cursoOptional = cursoService.buscarPorId(id);
        if (cursoOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensaje", "Curso no encontrado");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/cursos";
        }

        Curso curso = cursoOptional.get();

        if (!curso.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            redirectAttributes.addFlashAttribute("mensaje", "No tienes permiso para editar este curso");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/cursos";
        }

        while (curso.getRubros().size() < 5) {
            curso.agregarRubro(new Rubro());
        }
        model.addAttribute("curso", curso);
        model.addAttribute("cursosExistentes", cursoService.listarCursosPorUsuario(usuario));
        model.addAttribute("franjasHorarias", franjasHorarias);
        model.addAttribute("dias", dias);

        return "Cursos/formCurso";
    }

    // ----------------- ELIMINAR CURSO -----------------
    @GetMapping("/eliminar/{id}")
    public String eliminarCurso(@PathVariable Long id, Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String email = obtenerEmailDeAutenticacion(authentication);
            Usuario usuario = usuarioService.obtenerPorEmail(email);

            Optional<Curso> cursoOpt = cursoService.buscarPorId(id);
            if (cursoOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("mensaje", "Curso no encontrado");
                redirectAttributes.addFlashAttribute("tipo", "danger");
                return "redirect:/cursos";
            }

            Curso curso = cursoOpt.get();

            if (!curso.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
                redirectAttributes.addFlashAttribute("mensaje", "No tienes permiso para eliminar este curso");
                redirectAttributes.addFlashAttribute("tipo", "danger");
                return "redirect:/cursos";
            }

            cursoService.eliminarCurso(id);
            redirectAttributes.addFlashAttribute("mensaje", "Curso eliminado correctamente");
            redirectAttributes.addFlashAttribute("tipo", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al eliminar el curso: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/cursos";
    }

    // ── Helper: convierte "2-5 PM" → "2 PM – 5 PM", "11-2 PM" → "11 AM – 2 PM" etc.
    private String formatearHora(String hora) {
        if (hora == null) return "";
        Map<String, String> map = new LinkedHashMap<>();
        map.put("8-11",  "8 AM – 11 AM");
        map.put("11-2",  "11 AM – 2 PM");
        map.put("2-5",   "2 PM – 5 PM");
        map.put("6-9",   "6 PM – 9 PM");
        return map.getOrDefault(hora.trim(), hora);
    }
}