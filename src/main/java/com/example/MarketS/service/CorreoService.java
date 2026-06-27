package com.example.MarketS.service;

import com.example.MarketS.model.Correo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CorreoService {

    private static final Logger logger = LoggerFactory.getLogger(CorreoService.class);
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailFrom;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarCorreo(Correo correo) {
        try {
            for (String destinatario : correo.getDestinatarios()) {
                SimpleMailMessage mensaje = new SimpleMailMessage();
                mensaje.setFrom(mailFrom);
                mensaje.setTo(destinatario);
                mensaje.setSubject(correo.getAsunto());
                mensaje.setText(correo.getMensaje());
                mailSender.send(mensaje);
                logger.info("Correo enviado a {}", destinatario);
            }
        } catch (Exception e) {
            logger.error("Error enviando correo: {}", e.getMessage());
        }
    }
}
