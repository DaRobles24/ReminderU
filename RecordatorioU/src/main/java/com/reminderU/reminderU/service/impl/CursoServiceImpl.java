package com.reminderU.reminderU.service.impl;

import com.reminderU.reminderU.modelo.Curso;
import com.reminderU.reminderU.modelo.Rubro;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.repository.CursoRepository;
import com.reminderU.reminderU.service.CursoService;
import com.reminderU.reminderU.service.EmailService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CursoServiceImpl implements CursoService {

    private final CursoRepository cursoRepository;
    private final EmailService emailService;

    public CursoServiceImpl(CursoRepository cursoRepository, EmailService emailService) {
        this.cursoRepository = cursoRepository;
        this.emailService = emailService;
    }

    @Override
    public List<Curso> listarCursos() {
        return cursoRepository.findAll();
    }

    @Override
    public List<Curso> listarCursosPorUsuario(Usuario usuario) {
        return cursoRepository.findByUsuario(usuario);
    }

    @Override
    public Optional<Curso> buscarPorId(Long id) {
        return cursoRepository.findById(id);
    }

    @Override
    public Curso guardarCurso(Curso curso) {
        if (curso.getRubros() != null && !curso.getRubros().isEmpty()) {
            for (Rubro rubro : curso.getRubros()) {
                rubro.setCurso(curso);
            }
        }

        Curso cursoGuardado = cursoRepository.save(curso);

        // Enviar correo de forma asincrónica
        Usuario profesor = curso.getUsuario();
        if (profesor != null && profesor.getEmail() != null) {
            String asunto = "🎉 Nuevo curso agregado: " + curso.getNombre();
            String mensaje = "Hola " + profesor.getNombre() + " 👋,\n\n"
                    + "Has agregado un nuevo curso con los siguientes detalles:\n\n"
                    + "📚 Curso: " + curso.getNombre() + "\n"
                    + "👨‍🏫 Profesor: " + curso.getProfesor() + "\n"
                    + "📧 Correo: " + curso.getCorreo() + "\n"
                    + "⏰ Horario: " + curso.getHorario() + "\n\n"
                    + "🏫 Modalidad: " + curso.getModalidad() + "\n\n"
                    + "¡Éxitos en tu aprendizaje! 🚀\n\n"
                    + "Saludos,\nReminderU";
            emailService.enviarEmailAsync(profesor.getEmail(), asunto, mensaje);
        }

        return cursoGuardado;
    }

    @Override
    public void eliminarCurso(Long id) {
        Optional<Curso> cursoOpt = cursoRepository.findById(id);
        if (cursoOpt.isPresent()) {
            Curso curso = cursoOpt.get();
            cursoRepository.deleteById(id);

            // Enviar correo de eliminación de curso de forma asincrónica
            Usuario profesor = curso.getUsuario();
            if (profesor != null && profesor.getEmail() != null) {
                String asunto = "❌ Curso eliminado: " + curso.getNombre();
                String mensaje = "Hola " + profesor.getNombre() + " 👋,\n\n"
                        + "Te informamos que el curso 📚 \"" + curso.getNombre() + "\" ha sido eliminado de ReminderU.\n\n"
                        + "Si crees que esto es un error, por favor contacta con soporte.\n\n"
                        + "Saludos,\nReminderU";

                emailService.enviarEmailAsync(profesor.getEmail(), asunto, mensaje);
            }

        } else {
            throw new IllegalArgumentException("El curso con id " + id + " no existe.");
        }
    }
}
