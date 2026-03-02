package com.reminderU.reminderU.service.impl;

import com.reminderU.reminderU.modelo.Tarea;
import com.reminderU.reminderU.modelo.Usuario;
import com.reminderU.reminderU.repository.TareaRepository;
import com.reminderU.reminderU.service.EmailService;
import com.reminderU.reminderU.service.TareaService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class TareaServiceImpl implements TareaService {

    private final TareaRepository tareaRepository;
    private final EmailService emailService;

    public TareaServiceImpl(TareaRepository tareaRepository, EmailService emailService) {
        this.tareaRepository = tareaRepository;
        this.emailService = emailService;
    }

    @Override
    public List<Tarea> listarTareas() {
        return tareaRepository.findAll();
    }

    // Nuevo método: listar tareas filtradas por usuario
    public List<Tarea> listarTareasPorUsuario(Usuario usuario) {
        return tareaRepository.findByCurso_Usuario(usuario);
    }

    @Override
    public Optional<Tarea> buscarPorId(Long id) {
        return tareaRepository.findById(id);
    }

    @Override
    public Tarea guardarTarea(Tarea tarea) {
        Tarea saved = tareaRepository.save(tarea);

        // Enviar correo si se desea notificación inmediata al crear tarea
        if (saved.getCurso() != null && saved.getCurso().getUsuario() != null) {
            String email = saved.getCurso().getUsuario().getEmail();
            String asunto = "📝 Nueva tarea agregada: " + saved.getTitulo();

            StringBuilder mensaje = new StringBuilder();
            mensaje.append("👋 Hola ").append(saved.getCurso().getUsuario().getNombre()).append(",\n\n");
            mensaje.append("Se ha agregado una nueva tarea a tu curso 📚 \"")
                    .append(saved.getCurso().getNombre()).append("\".\n\n");
            mensaje.append("📝 Título: ").append(saved.getTitulo()).append("\n");
            mensaje.append("📅 Fecha de entrega: ").append(
                    saved.getFechaEntrega() != null ? saved.getFechaEntrega() : "No especificada"
            ).append("\n");
            mensaje.append("⏰ Hora de entrega: ").append(
                    saved.getHoraEntrega() != null ? saved.getHoraEntrega() : "No especificada"
            ).append("\n");
            mensaje.append("💯 Valor: ").append(
                    saved.getPorcentaje() != null ? saved.getPorcentaje() + "%" : "No especificado"
            ).append("\n\n");
            mensaje.append("¡No olvides completarla a tiempo! 🚀\n\n");
            mensaje.append("Saludos,\nReminderU ✨");

            emailService.enviarEmail(email, asunto, mensaje.toString());
        }

        return saved;
    }

    @Override
    public void eliminarTarea(Long id) {
        tareaRepository.deleteById(id);
    }

    @Override
    public List<Tarea> listarTareasPorCurso(Long cursoId) {
        return tareaRepository.findByCursoId(cursoId);
    }

    // Envío automático de recordatorios diarios a las 8:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    public void enviarRecordatorios() {
        List<Tarea> tareas = tareaRepository.findAll();
        LocalDate hoy = LocalDate.now();

        for (Tarea tarea : tareas) {
            if (tarea.getFechaEntrega() != null
                    && tarea.getCurso() != null
                    && tarea.getCurso().getUsuario() != null) {

                long diasRestantes = ChronoUnit.DAYS.between(hoy, tarea.getFechaEntrega());
                String email = tarea.getCurso().getUsuario().getEmail();
                String nombreUsuario = tarea.getCurso().getUsuario().getNombre();

                if (diasRestantes == 3 || diasRestantes == 2 || diasRestantes == 1) {
                    String asunto = "Recordatorio: tarea pendiente";
                    String mensaje = "Hola " + nombreUsuario + ",\n\n"
                            + "Te recordamos que la tarea \"" + tarea.getTitulo() + "\" vence en "
                            + diasRestantes + " día(s).\n\n¡No la dejes para último momento!\n\nSaludos,\nReminderU";
                    emailService.enviarEmail(email, asunto, mensaje);
                } else if (diasRestantes < 0) {
                    String asunto = "Tarea vencida";
                    String mensaje = "Hola " + nombreUsuario + ",\n\n"
                            + "La tarea \"" + tarea.getTitulo() + "\" ya ha vencido.\n\n"
                            + "Por favor revisa tus pendientes.\n\nSaludos,\nReminderU";
                    emailService.enviarEmail(email, asunto, mensaje);
                }
            }
        }
    }
}
