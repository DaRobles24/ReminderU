package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.service.impl.CustomOAuth2User;
import com.reminderU.reminderU.service.impl.CustomUserDetails;
import com.reminderU.reminderU.modelo.ConfiguracionCuatrimestre;
import com.reminderU.reminderU.service.ConfiguracionCuatrimestreService;
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
import java.util.*;

@Controller
@RequestMapping("/tareas")
public class TareaController {

    private final TareaServiceImpl tareaService;
    private final CursoServiceImpl cursoService;
    private final ConfiguracionCuatrimestreService cuatrimestreService;

    public TareaController(TareaServiceImpl tareaService,
                           CursoServiceImpl cursoService,
                           ConfiguracionCuatrimestreService cuatrimestreService) {
        this.tareaService = tareaService;
        this.cursoService = cursoService;
        this.cuatrimestreService = cuatrimestreService;
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

    // ─── Método auxiliar: cargar fechas del cuatrimestre al modelo ───────────
    private void cargarCuatrimestre(Model model, Usuario usuario) {
        ConfiguracionCuatrimestre cuatri = cuatrimestreService.getConfiguracionPorUsuario(usuario);
        if (cuatri != null && cuatri.getFechaInicio() != null && cuatri.getFechaFin() != null) {
            model.addAttribute("fechaInicioCuatrimestre", cuatri.getFechaInicio().toString());
            model.addAttribute("fechaFinCuatrimestre",    cuatri.getFechaFin().toString());
        }
    }

    // ─── Método auxiliar: construir cursosMap para el JS inline ──────────────
    private Map<Long, Map<String, String>> buildCursosMap(List<Curso> cursos) {
        Map<Long, Map<String, String>> cursosMap = new HashMap<>();
        for (Curso c : cursos) {
            Map<String, String> info = new HashMap<>();
            info.put("nombre", c.getNombre());
            info.put("dia", c.getDia() != null ? c.getDia().name() : "");
            info.put("hora", c.getHora() != null ? c.getHora() : "");
            info.put("modalidad", c.getModalidad() != null ? c.getModalidad().name() : "");
            cursosMap.put(c.getId(), info);
        }
        return cursosMap;
    }

    // ─── Clase interna para agrupar tareas por curso ─────────────────────────
    public static class GrupoCurso {
        private final Curso curso;
        private final List<Map<String, Object>> tareas;

        public GrupoCurso(Curso curso, List<Map<String, Object>> tareas) {
            this.curso = curso;
            this.tareas = tareas;
        }

        public Curso getCurso() { return curso; }
        public List<Map<String, Object>> getTareas() { return tareas; }
    }

    // ─── LISTAR ──────────────────────────────────────────────────────────────
    @GetMapping
    public String listarTareas(Model model) {
        Usuario usuarioLogueado = obtenerUsuarioActual();
        List<Tarea> todasTareas = tareaService.listarTareasPorUsuario(usuarioLogueado);

        // Separar pendientes y entregadas
        List<Tarea> pendientes = new ArrayList<>();
        List<Tarea> entregadas = new ArrayList<>();

        for (Tarea t : todasTareas) {
            if (t.getEstado() == null) t.setEstado(Tarea.Estado.PENDIENTE);
            if (t.getEstado() == Tarea.Estado.ENTREGADA) {
                entregadas.add(t);
            } else {
                pendientes.add(t);
            }
        }

        // Agrupar pendientes por curso
        model.addAttribute("cursosPendientes", agruparPorCurso(pendientes));

        // Agrupar entregadas por curso
        model.addAttribute("cursosEntregados", agruparPorCurso(entregadas));

        return "Tareas/listarTareas";
    }

    /**
     * Agrupa una lista de tareas por curso, calculando diasRestantes para cada una.
     */
    private List<GrupoCurso> agruparPorCurso(List<Tarea> tareas) {
        // Mapa ordenado: cursoId -> (curso, lista de tareas enriquecidas)
        Map<Long, Curso> cursosMap = new LinkedHashMap<>();
        Map<Long, List<Map<String, Object>>> tareasMap = new LinkedHashMap<>();

        LocalDate hoy = LocalDate.now();

        for (Tarea t : tareas) {
            if (t.getCurso() == null) continue;
            Long cursoId = t.getCurso().getId();

            cursosMap.putIfAbsent(cursoId, t.getCurso());
            tareasMap.putIfAbsent(cursoId, new ArrayList<>());

            Map<String, Object> tareaData = new HashMap<>();
            tareaData.put("id", t.getId());
            tareaData.put("titulo", t.getTitulo());
            tareaData.put("descripcion", t.getDescripcion());
            tareaData.put("fechaEntrega", t.getFechaEntrega());
            tareaData.put("horaEntrega", t.getHoraEntrega());
            tareaData.put("porcentaje", t.getPorcentaje());
            tareaData.put("estado", t.getEstado());
            tareaData.put("prioridad", t.getPrioridad());
            tareaData.put("modalidad", t.getModalidad());
            tareaData.put("notaObtenida", t.getNotaObtenida());
            tareaData.put("curso", t.getCurso());

            long diasRestantes = t.getFechaEntrega() != null
                    ? ChronoUnit.DAYS.between(hoy, t.getFechaEntrega()) : 0;
            tareaData.put("diasRestantes", diasRestantes);

            tareasMap.get(cursoId).add(tareaData);
        }

        List<GrupoCurso> grupos = new ArrayList<>();
        for (Long cursoId : cursosMap.keySet()) {
            grupos.add(new GrupoCurso(cursosMap.get(cursoId), tareasMap.get(cursoId)));
        }
        return grupos;
    }

    // ─── FORM NUEVA TAREA ────────────────────────────────────────────────────
    @GetMapping("/form")
    public String verFormTarea(Model model) {
        model.addAttribute("tarea", new Tarea());
        Usuario usuarioLogueado = obtenerUsuarioActual();
        List<Curso> cursos = cursoService.listarCursosPorUsuario(usuarioLogueado);
        model.addAttribute("cursos", cursos);
        model.addAttribute("cursosMap", buildCursosMap(cursos));
        cargarCuatrimestre(model, usuarioLogueado);
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

            List<Curso> cursos = cursoService.listarCursosPorUsuario(obtenerUsuarioActual());
            model.addAttribute("cursos", cursos);
            model.addAttribute("cursosMap", buildCursosMap(cursos));
            cargarCuatrimestre(model, obtenerUsuarioActual());

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

    // ─── GUARDAR NOTA ────────────────────────────────────────────────────────
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

    // ─── COMPLETAR TAREA ─────────────────────────────────────────────────────
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

    // ─── MARCAR COMO HECHA (GET legacy) ──────────────────────────────────────
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

    // ─── ENTREGAR (alias de completar, usado en la vista listarTareas) ────────
    @GetMapping("/entregar/{id}")
    public String entregarTarea(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        Optional<Tarea> opt = tareaService.buscarPorId(id);
        if (opt.isPresent()) {
            Tarea tarea = opt.get();
            tarea.setEstado(Tarea.Estado.ENTREGADA);
            tareaService.guardarTarea(tarea);
            redirectAttributes.addFlashAttribute("mensaje", "¡Tarea marcada como entregada! 🎉");
            redirectAttributes.addFlashAttribute("tipo", "success");
        } else {
            redirectAttributes.addFlashAttribute("mensaje", "Tarea no encontrada");
            redirectAttributes.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/tareas";
    }
}