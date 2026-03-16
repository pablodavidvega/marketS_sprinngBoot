package com.example.MarketS.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void enviarCorreo(String para, String asunto, String mensaje) {

        try {

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom("MarketS@gmail.com"); // mismo correo configurado en Render
            mail.setTo(para);
            mail.setSubject(asunto);
            mail.setText(mensaje);

            mailSender.send(mail);

            System.out.println("Correo enviado correctamente a: " + para);

        } catch (Exception e) {

            System.out.println("Error al enviar correo a: " + para);
            System.out.println("Motivo: " + e.getMessage());
            e.printStackTrace();

        }
    }
}