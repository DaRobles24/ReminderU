package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.HoraFranja;
import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.Evaluacion;
import com.reminderU.reminderU.modelo.Rubro;
import com.reminderU.reminderU.modelo.Universidad;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.repository.UsuarioRepository;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cursos")
public class CursoController {

    private final CursoServiceImpl cursoService;
    private final TareaServiceImpl tareaService;
    private final EvaluacionServiceImpl evaluacionService;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public CursoController(CursoServiceImpl cursoService, TareaServiceImpl tareaService,
                           EvaluacionServiceImpl evaluacionService, UsuarioService usuarioService,
                           UsuarioRepository usuarioRepository) {
        this.cursoService       = cursoService;
        this.tareaService       = tareaService;
        this.evaluacionService  = evaluacionService;
        this.usuarioService     = usuarioService;
        this.usuarioRepository  = usuarioRepository;
    }

    // ─── Email del principal ──────────────────────────────────────────────────
    private String obtenerEmailDeAutenticacion(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User) {
            return ((OAuth2User) authentication.getPrincipal()).getAttribute("email");
        }
        return authentication.getName();
    }

    // ─── Usuario con universidad + franjas + días cargados (evita lazy) ───────
    private Usuario obtenerUsuarioConUniversidad(Authentication authentication) {
        String email = obtenerEmailDeAutenticacion(authentication);
        return usuarioRepository.findByEmailConUniversidad(email);
    }

    // ─── Franjas dinámicas según universidad del usuario ─────────────────────
    private List<String> obtenerFranjas(Usuario usuario) {
        Universidad uni = usuario.getUniversidad();
        if (uni == null || uni.getFranjas() == null || uni.getFranjas().isEmpty()) {
            return List.of("8-11", "11-2", "2-5", "5-8", "6-9");
        }
        return uni.getFranjas().stream()
                .map(HoraFranja::getEtiqueta)
                .collect(Collectors.toList());
    }

    // ─── Días dinámicos según universidad del usuario ─────────────────────────
    private List<String> obtenerDias(Usuario usuario) {
        Universidad uni = usuario.getUniversidad();
        if (uni == null || uni.getDias() == null || uni.getDias().isEmpty()) {
            return List.of("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO");
        }
        return uni.getDias().stream()
                .map(d -> d.getDia().name())
                .collect(Collectors.toList());
    }

    // ─── LISTAR ───────────────────────────────────────────────────────────────
    @GetMapping
    public String listarCursos(Authentication authentication, Model model) {
        Usuario usuario = obtenerUsuarioConUniversidad(authentication);
        if (usuario == null) return "redirect:/login";

        List<String> franjasHorarias = obtenerFranjas(usuario);
        List<String> dias            = obtenerDias(usuario);
        List<Curso>  cursos          = cursoService.listarCursosPorUsuario(usuario);

        Map<String, Map<String, Curso>> mapaCursos = new HashMap<>();
        for (String dia : dias) {
            mapaCursos.put(dia, new HashMap<>());
            for (String hora : franjasHorarias) mapaCursos.get(dia).put(hora, null);
        }
        for (Curso curso : cursos) {
            if (curso.getDia() != null && curso.getHora() != null) {
                String diaKey  = curso.getDia().name();
                String horaKey = curso.getHora().trim();
                if (mapaCursos.containsKey(diaKey) && mapaCursos.get(diaKey).containsKey(horaKey)) {
                    mapaCursos.get(diaKey).put(horaKey, curso);
                }
            }
        }

        model.addAttribute("cursosExistentes",  cursos);
        model.addAttribute("mapaCursos",         mapaCursos);
        model.addAttribute("dias",               dias);
        model.addAttribute("franjasHorarias",    franjasHorarias);
        return "Cursos/curso";
    }

    // ─── VER CURSO ────────────────────────────────────────────────────────────
    @GetMapping("/ver/{id}")
    public String verCurso(@PathVariable Long id, Authentication authentication,
                           Model model, RedirectAttributes redirectAttributes) {
        Usuario usuario = obtenerUsuarioConUniversidad(authentication);

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
        model.addAttribute("horaFormateada", formatearHora(curso.getHora()));

        List<Tarea> todasTareas    = tareaService.listarTareasPorCurso(id);
        List<Tarea> tareasPendientes  = new ArrayList<>();
        List<Tarea> tareasEntregadas  = new ArrayList<>();
        for (Tarea t : todasTareas) {
            if (t.getEstado() == Tarea.Estado.ENTREGADA) tareasEntregadas.add(t);
            else tareasPendientes.add(t);
        }
        model.addAttribute("tareasPendientes", tareasPendientes);
        model.addAttribute("tareasEntregadas", tareasEntregadas);

        List<Evaluacion> desgloseEvaluaciones = evaluacionService.listarEvaluacionesPorCurso(id);
        model.addAttribute("desgloseEvaluaciones", desgloseEvaluaciones);

        double  puntosGanados = 0.0;
        boolean hayNotas      = false;

        List<Evaluacion> conNota = desgloseEvaluaciones.stream()
                .filter(e -> e.getNotaObtenida() != null).toList();

        if (!conNota.isEmpty()) {
            for (Evaluacion ev : conNota)
                puntosGanados += ev.getNotaObtenida() * (ev.getPorcentaje() / 100.0);
            hayNotas = true;
        } else {
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
            boolean aprobando     = notaPromedio >= 70.0;
            model.addAttribute("notaPromedio",   notaPromedio);
            model.addAttribute("puntosParaMeta", puntosParaMeta);
            model.addAttribute("barraProgreso",  Math.min(notaPromedio, 100.0));
            model.addAttribute("aprobando",      aprobando);
            model.addAttribute("hayNotas",       true);
        }

        return "Cursos/verCursos";
    }

    // ─── FORMULARIO ───────────────────────────────────────────────────────────
    @GetMapping("/form")
    public String verFormCurso(@RequestParam(value = "id", required = false) Long id,
                               Authentication authentication, Model model) {
        Usuario usuario = obtenerUsuarioConUniversidad(authentication);

        Curso curso = (id != null) ? cursoService.buscarPorId(id).orElse(new Curso()) : new Curso();
        while (curso.getRubros().size() < 5) curso.agregarRubro(new Rubro());

        model.addAttribute("curso",            curso);
        model.addAttribute("cursosExistentes", cursoService.listarCursosPorUsuario(usuario));
        model.addAttribute("franjasHorarias",  obtenerFranjas(usuario));
        model.addAttribute("dias",             obtenerDias(usuario));
        return "Cursos/formCurso";
    }

    // ─── GUARDAR ──────────────────────────────────────────────────────────────
    @PostMapping("/guardar")
    public String guardarCurso(@ModelAttribute Curso curso, Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = obtenerUsuarioConUniversidad(authentication);
            if (usuario == null) return "redirect:/login";

            if (curso.getHora() != null) curso.setHora(curso.getHora().trim());

            Optional<Curso> existente = cursoService.listarCursosPorUsuario(usuario).stream()
                    .filter(c -> c.getDia().equals(curso.getDia())
                            && c.getHora().equals(curso.getHora())
                            && (curso.getId() == null || !c.getId().equals(curso.getId())))
                    .findFirst();

            if (existente.isPresent()) {
                redirectAttributes.addFlashAttribute("mensaje", "¡Ya existe un curso en este horario!");
                redirectAttributes.addFlashAttribute("tipo", "danger");
                return "redirect:/cursos/form";
            }

            curso.setUsuario(usuario);
            cursoService.guardarCurso(curso);
            redirectAttributes.addFlashAttribute("mensaje", "Curso guardado correctamente");
            redirectAttributes.addFlashAttribute("tipo", "success");
            return "redirect:/cursos";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("mensaje", "Error al guardar el curso: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/cursos/form";
        }
    }

    // ─── EDITAR ───────────────────────────────────────────────────────────────
    @GetMapping("/editar/{id}")
    public String editarCurso(@PathVariable Long id, Authentication authentication,
                              Model model, RedirectAttributes redirectAttributes) {
        Usuario usuario = obtenerUsuarioConUniversidad(authentication);

        Optional<Curso> cursoOpt = cursoService.buscarPorId(id);
        if (cursoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensaje", "Curso no encontrado");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/cursos";
        }

        Curso curso = cursoOpt.get();
        if (!curso.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())) {
            redirectAttributes.addFlashAttribute("mensaje", "No tienes permiso para editar este curso");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/cursos";
        }

        while (curso.getRubros().size() < 5) curso.agregarRubro(new Rubro());

        model.addAttribute("curso",            curso);
        model.addAttribute("cursosExistentes", cursoService.listarCursosPorUsuario(usuario));
        model.addAttribute("franjasHorarias",  obtenerFranjas(usuario));
        model.addAttribute("dias",             obtenerDias(usuario));
        return "Cursos/formCurso";
    }

    // ─── ELIMINAR ─────────────────────────────────────────────────────────────
    @GetMapping("/eliminar/{id}")
    public String eliminarCurso(@PathVariable Long id, Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = obtenerUsuarioConUniversidad(authentication);

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
            redirectAttributes.addFlashAttribute("mensaje", "Error al eliminar: " + e.getMessage());
            redirectAttributes.addFlashAttribute("tipo", "danger");
        }
        return "redirect:/cursos";
    }

    // ─── Helper formatear hora ────────────────────────────────────────────────
    private String formatearHora(String hora) {
        if (hora == null) return "";
        Map<String, String> map = new LinkedHashMap<>();
        map.put("8-11",  "8 AM – 11 AM");
        map.put("11-2",  "11 AM – 2 PM");
        map.put("2-5",   "2 PM – 5 PM");
        map.put("5-8",   "5 PM – 8 PM");
        map.put("6-9",   "6 PM – 9 PM");
        map.put("8-12",  "8 AM – 12 PM");
        map.put("12-4",  "12 PM – 4 PM");
        map.put("4-8",   "4 PM – 8 PM");
        map.put("6-10",  "6 PM – 10 PM");
        return map.getOrDefault(hora.trim(), hora);
    }
}