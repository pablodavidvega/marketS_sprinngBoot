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

    @Value("${spring.mail.username:pablo.david.vega1@gmail.com}")
    private String emailAccount;

    @Async
    public void enviarCorreo(String para, String asunto, String mensaje) {
        System.out.println("🔵 INTENTANDO enviar correo a: " + para);
        System.out.println("🔵 Asunto: " + asunto);
        System.out.println("🔵 Usando cuenta: " + emailAccount);
        System.out.println("🔵 MailSender: " + mailSender.getClass().getName());

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(emailAccount);
            mail.setTo(para);
            mail.setSubject(asunto);
            mail.setText(mensaje);

            System.out.println("🟡 Intentando enviar con JavaMailSender...");
            mailSender.send(mail);
            System.out.println("✅ CORREO ENVIADO EXITOSAMENTE a: " + para);

        } catch (Exception e) {
            System.out.println("❌ ERROR EN ENVÍO DE CORREO");
            System.out.println("❌ Tipo de error: " + e.getClass().getName());
            System.out.println("❌ Mensaje: " + e.getMessage());
            e.printStackTrace();  // Esto mostrará el error completo
        }
    }
}