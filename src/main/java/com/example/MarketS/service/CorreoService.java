package com.example.MarketS.service;

import com.example.MarketS.model.Correo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class CorreoService {

    private static final Logger logger = LoggerFactory.getLogger(CorreoService.class);

    private final JavaMailSender mailSender;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    public CorreoService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void enviarCorreo(Correo correo) {
        try {
            for (String destinatario : correo.getDestinatarios()) {
                if (resendApiKey != null && !resendApiKey.isBlank()) {
                    enviarViaResend(destinatario, correo.getAsunto(), correo.getMensaje());
                } else {
                    enviarViaSmtp(destinatario, correo.getAsunto(), correo.getMensaje());
                }
                logger.info("Correo enviado a {}", destinatario);
            }
        } catch (Exception e) {
            logger.error("Error enviando correo: {}", e.getMessage());
        }
    }

    private void enviarViaResend(String to, String subject, String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resendApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
            "from", mailFrom,
            "to", List.of(to),
            "subject", subject,
            "text", text
        );

        String response = restTemplate.postForObject(
            "https://api.resend.com/emails",
            new HttpEntity<>(body, headers),
            String.class
        );
        logger.info("Resend response para {}: {}", to, response);
    }

    private void enviarViaSmtp(String to, String subject, String text) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(mailFrom);
        mensaje.setTo(to);
        mensaje.setSubject(subject);
        mensaje.setText(text);
        mailSender.send(mensaje);
    }
}
