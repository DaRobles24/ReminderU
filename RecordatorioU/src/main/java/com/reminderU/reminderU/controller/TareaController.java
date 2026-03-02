package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.service.impl.CustomOAuth2User;
import com.reminderU.reminderU.service.impl.CustomUserDetails;
import com.reminderU.reminderU.service.impl.CursoServiceImpl;
import com.reminderU.reminderU.service.impl.TareaServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
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

    private Usuario obtenerUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getUsuario();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getUsuario();
        }
        throw new IllegalStateException("Tipo de principal desconocido: " + principal.getClass());
    }

    // ─── LISTAR ──────────────────────────────────────────────────────────────
    @GetMapping
    public String listarTareas(Model model) {
        Usuario usuarioLogueado = obtenerUsuarioActual();
        List<Tarea> tareas = tareaService.listarTareasPorUsuario(usuarioLogueado);

        List<Map<String, Object>> tareasConTiempo = tareas.stream().map(t -> {
            Map<String, Object> mapa = new HashMap<>();
            mapa.put("tarea", t);

            java.time.LocalDateTime ahora = java.time.LocalDateTime.now();
            java.time.LocalDateTime entrega = (t.getFechaEntrega() != null && t.getHoraEntrega() != null)
                    ? java.time.LocalDateTime.of(t.getFechaEntrega(), t.getHoraEntrega())
                    : java.time.LocalDateTime.of(
                        t.getFechaEntrega() != null ? t.getFechaEntrega() : LocalDate.now(),
                        LocalTime.of(23, 59));

            long totalMinutos = ChronoUnit.MINUTES.between(ahora, entrega);
            long dias    = totalMinutos / (60 * 24);
            long horas   = (totalMinutos % (60 * 24)) / 60;
            long minutos = totalMinutos % 60;

            mapa.put("dias", dias);
            mapa.put("horas", horas);
            mapa.put("minutos", minutos);

            String color;
            if (totalMinutos < 0)  color = "text-danger";
            else if (dias > 4)     color = "text-success";
            else if (dias >= 1)    color = "text-warning";
            else                   color = "text-danger alerta-urgente";
            mapa.put("colorTiempo", color);

            if (t.getEstado() == null) t.setEstado(Tarea.Estado.PENDIENTE);
            return mapa;
        }).toList();

        model.addAttribute("tareasConTiempo", tareasConTiempo);
        return "Tareas/listarTareas";
    }

    // ─── FORM NUEVA TAREA ────────────────────────────────────────────────────
    @GetMapping("/form")
    public String verFormTarea(Model model) {
        model.addAttribute("tarea", new Tarea());
        Usuario usuarioLogueado = obtenerUsuarioActual();
        model.addAttribute("cursos", cursoService.listarCursosPorUsuario(usuarioLogueado));
        return "Tareas/formTarea";
    }

    // ─── GUARDAR TAREA ───────────────────────────────────────────────────────
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

    // ─── EDITAR TAREA ────────────────────────────────────────────────────────
    @GetMapping("/editar/{id}")
    public String editarTarea(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Tarea> tareaOptional = tareaService.buscarPorId(id);
        if (tareaOptional.isPresent()) {
            Tarea tarea = tareaOptional.get();
            if (tarea.getFechaEntrega() == null) tarea.setFechaEntrega(LocalDate.now());
            if (tarea.getHoraEntrega() == null)  tarea.setHoraEntrega(LocalTime.of(23, 59));
            model.addAttribute("tarea", tarea);
            model.addAttribute("cursos", cursoService.listarCursosPorUsuario(obtenerUsuarioActual()));
            return "Tareas/formTarea";
        }
        redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
        redirectAttributes.addFlashAttribute("tipo", "danger");
        return "redirect:/tareas";
    }

    // ─── VER TAREA ───────────────────────────────────────────────────────────
    @GetMapping("/ver/{id}")
    public String verTarea(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Tarea> tareaOptional = tareaService.buscarPorId(id);
        if (tareaOptional.isPresent()) {
            Tarea tarea = tareaOptional.get();
            model.addAttribute("tarea", tarea);
            long diasRestantes = tarea.getFechaEntrega() != null
                    ? ChronoUnit.DAYS.between(LocalDate.now(), tarea.getFechaEntrega()) : 0;
            model.addAttribute("diasRestantes", diasRestantes);
            return "Tareas/verTarea";
        }
        redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
        redirectAttributes.addFlashAttribute("tipo", "danger");
        return "redirect:/tareas";
    }

    // ─── GUARDAR NOTA (POST, sin fetch, sin problema CSRF) ───────────────────
    @PostMapping("/nota/{id}")
    public String guardarNota(@PathVariable Long id,
                              @RequestParam("nota") Double nota,
                              RedirectAttributes redirectAttributes) {
        if (nota < 0 || nota > 100) {
            redirectAttributes.addFlashAttribute("mensaje", "La nota debe estar entre 0 y 100");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/tareas/ver/" + id;
        }
        Optional<Tarea> opt = tareaService.buscarPorId(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/tareas";
        }
        Tarea tarea = opt.get();
        tarea.setNotaObtenida(nota);
        tareaService.guardarTarea(tarea);
        redirectAttributes.addFlashAttribute("mensaje", "Nota guardada correctamente ✅");
        redirectAttributes.addFlashAttribute("tipo", "success");
        return "redirect:/tareas/ver/" + id;
    }

    // ─── COMPLETAR TAREA (POST, sin fetch, sin problema CSRF) ────────────────
    @PostMapping("/completar/{id}")
    public String completarTarea(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Tarea> opt = tareaService.buscarPorId(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/tareas";
        }
        Tarea tarea = opt.get();
        tarea.setEstado(Tarea.Estado.ENTREGADA);
        tareaService.guardarTarea(tarea);
        redirectAttributes.addFlashAttribute("mensaje", "¡Tarea marcada como completada! 🎉");
        redirectAttributes.addFlashAttribute("tipo", "success");
        return "redirect:/tareas/ver/" + id;
    }

    // ─── ELIMINAR ────────────────────────────────────────────────────────────
    @GetMapping("/eliminar/{id}")
    public String eliminarTarea(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        tareaService.eliminarTarea(id);
        redirectAttributes.addFlashAttribute("mensaje", "Tarea eliminada correctamente");
        redirectAttributes.addFlashAttribute("tipo", "success");
        return "redirect:/tareas";
    }

    // ─── MARCAR COMO HECHA (GET legacy, por si acaso) ────────────────────────
    @GetMapping("/marcar/{id}")
    public String marcarComoHecha(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Tarea> opt = tareaService.buscarPorId(id);
        if (opt.isPresent()) {
            Tarea tarea = opt.get();
            tarea.setEstado(Tarea.Estado.ENTREGADA);
            tareaService.guardarTarea(tarea);
            redirectAttributes.addFlashAttribute("mensaje", "Tarea marcada como hecha");
            redirectAttributes.addFlashAttribute("tipo", "success");
            return "redirect:/tareas/ver/" + id;
        }
        redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
        redirectAttributes.addFlashAttribute("tipo", "danger");
        return "redirect:/tareas";
    }
}