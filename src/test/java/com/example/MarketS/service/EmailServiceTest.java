package com.example.MarketS.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@SpringBootTest
class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @Test
    void testEnvioCorreo() throws Exception {

        emailService.enviarCorreo(
                "usuario@test.com",
                "Prueba MarketS",
                "Correo de prueba"
        );

        // Espera para que el método @Async se ejecute
        Thread.sleep(500);

        Mockito.verify(mailSender)
                .send(Mockito.any(SimpleMailMessage.class));
    }
}