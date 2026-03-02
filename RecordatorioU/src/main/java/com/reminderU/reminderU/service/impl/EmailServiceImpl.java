package com.reminderU.reminderU.service.impl;

import com.reminderU.reminderU.service.EmailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void enviarEmail(String para, String asunto, String cuerpo) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
        } catch (Exception e) {
            System.out.println("Error enviando correo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    @Override
    public void enviarEmailAsync(String para, String asunto, String cuerpo) {
        enviarEmail(para, asunto, cuerpo);
    }
}
