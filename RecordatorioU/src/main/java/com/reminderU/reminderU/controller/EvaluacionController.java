package com.reminderU.reminderU.controller;

import com.reminderU.reminderU.modelo.Evaluacion;
import com.reminderU.reminderU.service.impl.EvaluacionServiceImpl;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/evaluaciones")
public class EvaluacionController {

    private final EvaluacionServiceImpl evaluacionService;

    public EvaluacionController(EvaluacionServiceImpl evaluacionService) {
        this.evaluacionService = evaluacionService;
    }

    /**
     * Guarda la nota obtenida de una evaluación (0–100).
     * Redirige de vuelta al curso al que pertenece.
     */
    @PostMapping("/nota/{id}")
    public String guardarNota(@PathVariable Long id,
                              @RequestParam("nota") Double nota,
                              RedirectAttributes redirectAttributes) {

        if (nota < 0 || nota > 100) {
            redirectAttributes.addFlashAttribute("mensaje", "La nota debe estar entre 0 y 100");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            // Intentar redirigir al curso si lo encontramos
            Optional<Evaluacion> opt = evaluacionService.buscarPorId(id);
            if (opt.isPresent() && opt.get().getCurso() != null) {
                return "redirect:/cursos/ver/" + opt.get().getCurso().getId();
            }
            return "redirect:/cursos";
        }

        Optional<Evaluacion> opt = evaluacionService.buscarPorId(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("mensaje", "Evaluación no encontrada");
            redirectAttributes.addFlashAttribute("tipo", "danger");
            return "redirect:/cursos";
        }

        Evaluacion ev = opt.get();
        ev.setNotaObtenida(nota);
        evaluacionService.guardarEvaluacion(ev);

        redirectAttributes.addFlashAttribute("mensaje", "Nota guardada correctamente ✅");
        redirectAttributes.addFlashAttribute("tipo", "success");

        return "redirect:/cursos/ver/" + ev.getCurso().getId();
    }
}