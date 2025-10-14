package com.example.MarketS.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
public class CorreoService {

    private static final Logger logger = LoggerFactory.getLogger(CorreoService.class);
    private final JavaMailSender mailSender;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarCorreo(String para, String asunto, String texto) {
        logger.info("📤 Iniciando envío de correo a {}", para);
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom("tu_correo@gmail.com"); // 👈 importante definir remitente
            mensaje.setTo(para);
            mensaje.setSubject(asunto);
            mensaje.setText(texto);
            mailSender.send(mensaje);
            logger.info("✅ Correo enviado correctamente a {}", para);
        } catch (Exception e) {
            logger.error("❌ Error enviando correo a {}: {}", para, e.getMessage());
        }
    }

    @Async
    public void enviarCorreoMasivo(List<String> destinatarios, String asunto, String texto) {
        for (String para : destinatarios) {
            try {
                enviarCorreo(para, asunto, texto);
            } catch (Exception e) {
                System.err.println("Error enviando a " + para + ": " + e.getMessage());
            }
        }
    }

}
