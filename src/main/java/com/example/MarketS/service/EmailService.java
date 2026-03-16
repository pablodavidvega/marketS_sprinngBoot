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

    // Definimos la variable emailAccount
    @Value("${spring.mail.username:pablo.david.vega1@gmail.com}")
    private String emailAccount;  // 👈 Esta es la variable que faltaba

    @Async
    public void enviarCorreo(String para, String asunto, String mensaje) {
        System.out.println("🔵 INTENTANDO enviar correo a: " + para);
        System.out.println("🔵 Asunto: " + asunto);
        System.out.println("🔵 Usando cuenta: " + emailAccount);

        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(emailAccount);  // 👈 Ahora sí existe la variable
            mail.setTo(para);
            mail.setSubject(asunto);
            mail.setText(mensaje);

            mailSender.send(mail);

            System.out.println("✅ Correo ENVIADO correctamente a: " + para);

        } catch (Exception e) {
            System.out.println("❌ ERROR al enviar correo a: " + para);
            System.out.println("❌ Mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
}