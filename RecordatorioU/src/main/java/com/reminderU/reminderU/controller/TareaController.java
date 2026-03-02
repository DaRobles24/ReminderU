package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.service.impl.CustomOAuth2User;
import com.reminderU.reminderU.service.impl.CustomUserDetails;
import com.reminderU.reminderU.service.impl.CursoServiceImpl;
import com.reminderU.reminderU.service.impl.TareaServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/tareas")
public class TareaController {

    private final TareaServiceImpl tareaService;
    private final CursoServiceImpl cursoService;

    public TareaController(TareaServiceImpl tareaService, CursoServiceImpl cursoService) {
        this.tareaService = tareaService;
        this.cursoService = cursoService;
    }

    /**
     * Método auxiliar para obtener el Usuario desde el Authentication
     * Maneja tanto usuarios locales (CustomUserDetails) como OAuth2 (CustomOAuth2User)
     */
    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        
        if (principal instanceof CustomUserDetails) {
            // Usuario con login local
            return ((CustomUserDetails) principal).getUsuario();
        } else if (principal instanceof CustomOAuth2User) {
            // Usuario con login OAuth2 (Google)
            return ((CustomOAuth2User) principal).getUsuario();
        }
        
        throw new IllegalStateException("Tipo de principal desconocido: " + principal.getClass());
    }

    // Listar todas las tareas del usuario logueado
    @GetMapping
    public String listarTareas(Model model) {
        Usuario usuarioLogueado = obtenerUsuarioActual();

        List<Tarea> tareas = tareaService.listarTareasPorUsuario(usuarioLogueado);

        List<Map<String, Object>> tareasConTiempo = tareas.stream().map(t -> {
            Map<String, Object> mapa = new HashMap<>();
            mapa.put("tarea", t);

            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime entrega = t.getFechaEntrega() != null && t.getHoraEntrega() != null
                    ? LocalDateTime.of(t.getFechaEntrega(), t.getHoraEntrega())
                    : LocalDateTime.of(t.getFechaEntrega() != null ? t.getFechaEntrega() : LocalDate.now(), LocalTime.of(23, 59));

            long totalMinutos = ChronoUnit.MINUTES.between(ahora, entrega);
            long dias = totalMinutos / (60 * 24);
            long horas = (totalMinutos % (60 * 24)) / 60;
            long minutos = totalMinutos % 60;

            mapa.put("dias", dias);
            mapa.put("horas", horas);
            mapa.put("minutos", minutos);

            String color;
            if (totalMinutos < 0) {
                color = "text-danger";
            } else if (dias > 4) {
                color = "text-success";
            } else if (dias <= 4 && dias >= 1) {
                color = "text-warning";
            } else {
                color = "text-danger alerta-urgente";
            }
            mapa.put("colorTiempo", color);

            if (t.getEstado() == null) {
                t.setEstado(Tarea.Estado.PENDIENTE);
            }

            return mapa;
        }).toList();

        model.addAttribute("tareasConTiempo", tareasConTiempo);
        return "Tareas/listarTareas";
    }

    // Formulario para nueva tarea
    @GetMapping("/form")
    public String verFormTarea(Model model) {
        model.addAttribute("tarea", new Tarea());

        Usuario usuarioLogueado = obtenerUsuarioActual();

        List<Curso> cursos = cursoService.listarCursosPorUsuario(usuarioLogueado);
        model.addAttribute("cursos", cursos);

        return "Tareas/formTarea";
    }

    // Guardar tarea
    @PostMapping("/guardar")
    public String guardarTarea(@ModelAttribute Tarea tarea, RedirectAttributes redirectAttributes) {
        if (tarea.getCurso() != null && tarea.getCurso().getId() != null) {
            Curso curso = cursoService.buscarPorId(tarea.getCurso().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Curso no existe"));
            tarea.setCurso(curso);
        } else {
            throw new IllegalArgumentException("Debe seleccionar un curso válido");
        }

        tareaService.guardarTarea(tarea);
        redirectAttributes.addFlashAttribute("mensaje", "Tarea guardada correctamente");
        redirectAttributes.addFlashAttribute("tipo", "success");
        return "redirect:/tareas";
    }

    // Editar tarea con datos precargados
    @GetMapping("/editar/{id}")
    public String editarTarea(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Tarea> tareaOptional = tareaService.buscarPorId(id);
        if (tareaOptional.isPresent()) {
            Tarea tarea = tareaOptional.get();

            if (tarea.getFechaEntrega() == null) {
                tarea.setFechaEntrega(LocalDate.now());
            }
            if (tarea.getHoraEntrega() == null) {
                tarea.setHoraEntrega(LocalTime.of(23, 59));
            }

            model.addAttribute("tarea", tarea);

            Usuario usuarioLogueado = obtenerUsuarioActual();
            List<Curso> cursos = cursoService.listarCursosPorUsuario(usuarioLogueado);
            model.addAttribute("cursos", cursos);

            return "Tareas/formTarea";
        } else {
            redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/tareas";
        }
    }

    // Ver tarea específica
    @GetMapping("/ver/{id}")
    public String verTarea(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Tarea> tareaOptional = tareaService.buscarPorId(id);
        if (tareaOptional.isPresent()) {
            Tarea tarea = tareaOptional.get();
            model.addAttribute("tarea", tarea);

            LocalDate hoy = LocalDate.now();
            long diasRestantes = ChronoUnit.DAYS.between(hoy, tarea.getFechaEntrega());
            model.addAttribute("diasRestantes", diasRestantes);

            String color;
            if (diasRestantes < 0) {
                color = "text-danger";
            } else if (diasRestantes > 4) {
                color = "text-success";
            } else if (diasRestantes == 3) {
                color = "text-warning";
            } else {
                color = "text-danger alerta-urgente";
            }
            model.addAttribute("colorTiempo", color);

            return "Tareas/verTarea";
        } else {
            redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/tareas";
        }
    }

    // Marcar como hecha
    @GetMapping("/marcar/{id}")
    public String marcarComoHecha(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Tarea> tareaOptional = tareaService.buscarPorId(id);
        if (tareaOptional.isPresent()) {
            Tarea tarea = tareaOptional.get();
            tarea.setEstado(Tarea.Estado.ENTREGADA);
            tareaService.guardarTarea(tarea);

            redirectAttributes.addFlashAttribute("mensaje", "Tarea marcada como hecha");
            redirectAttributes.addFlashAttribute("tipo", "success");
            return "redirect:/tareas/ver/" + id;
        } else {
            redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/tareas";
        }
    }

    // Marcar como hecha vía fetch
    @PostMapping("/tareas/marcar-hecha/{id}")
    @ResponseBody
    public Map<String, Object> marcarComoHechaPost(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        Optional<Tarea> tareaOptional = tareaService.buscarPorId(id);

        if (tareaOptional.isPresent()) {
            Tarea tarea = tareaOptional.get();
            tarea.setEstado(Tarea.Estado.ENTREGADA);
            tareaService.guardarTarea(tarea);

            response.put("status", "success");
            response.put("mensaje", "Tarea marcada como entregada ✅");
            response.put("titulo", tarea.getTitulo());
            response.put("descripcion", tarea.getDescripcion());
            response.put("curso", tarea.getCurso().getNombre());
            response.put("notaObtenida", tarea.getNotaObtenida());
            response.put("porcentaje", tarea.getPorcentaje());
        } else {
            response.put("status", "error");
            response.put("mensaje", "⚠️ Tarea no encontrada");
        }

        return response;
    }

    // Actualizar nota
    @PutMapping("/api/{id}/nota")
    @ResponseBody
    public ResponseEntity<?> actualizarNota(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        try {
            Double nota = Double.valueOf(request.get("nota").toString());

            if (nota < 0 || nota > 100) {
                return ResponseEntity.badRequest().body("La nota debe estar entre 0 y 100");
            }

            Optional<Tarea> tareaOptional = tareaService.buscarPorId(id);
            if (tareaOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Tarea tarea = tareaOptional.get();
            tarea.setNotaObtenida(nota);
            tareaService.guardarTarea(tarea);

            return ResponseEntity.ok().body("Nota actualizada correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar la nota: " + e.getMessage());
        }
    }

    // Marcar completada
    @PutMapping("/api/{id}/completar")
    @ResponseBody
    public ResponseEntity<?> marcarCompletada(@PathVariable Long id) {
        try {
            Optional<Tarea> tareaOptional = tareaService.buscarPorId(id);
            if (tareaOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Tarea tarea = tareaOptional.get();
            if (tarea.getNotaObtenida() == null || tarea.getNotaObtenida() == 0) {
                return ResponseEntity.badRequest().body("La tarea debe tener una nota antes de ser completada");
            }

            tarea.setEstado(Tarea.Estado.ENTREGADA);
            tareaService.guardarTarea(tarea);

            return ResponseEntity.ok().body("Tarea marcada como completada");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al completar la tarea: " + e.getMessage());
        }
    }

    // Endpoint API para crear tarea vía JSON (AJAX)
    @PostMapping(value = "/api/tareas", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<?> crearTareaApi(@RequestBody Tarea tarea) {
        try {
            // Si el objeto Tarea viene sin curso o usuario, el servicio debe resolverlo; aquí asumimos que viene con lo mínimo necesario
            Tarea saved = tareaService.guardarTarea(tarea);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear la tarea: " + e.getMessage());
        }
    }

}
