package com.reminderU.reminderU.service;

public interface EmailService {
    void enviarEmail(String destino, String asunto, String mensaje);
    void enviarEmailAsync(String destino, String asunto, String mensaje);
}
