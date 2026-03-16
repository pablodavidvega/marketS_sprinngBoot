package com.example.MarketS.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Usuario para autenticación (puede ser diferente al remitente)
    @Value("${spring.mail.username:pablo.david.vega1@gmail.com}")
    private String mailUsername;

    // Remitente que aparecerá en el correo
    @Value("${spring.mail.from:${spring.mail.username}}")
    private String fromEmail;

    @Async
    public void enviarCorreo(String para, String asunto, String mensaje) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);  // 👈 Usamos fromEmail, no mailUsername
            mail.setTo(para);
            mail.setSubject(asunto);
            mail.setText(mensaje);

            mailSender.send(mail);

            System.out.println("Correo enviado correctamente a: " + para);
            System.out.println("Remitente: " + fromEmail);

        } catch (Exception e) {
            System.out.println("Error al enviar correo a: " + para);
            System.out.println("Motivo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}