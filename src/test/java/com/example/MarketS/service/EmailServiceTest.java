package com.example.MarketS.service;

import com.sendgrid.SendGrid;
import com.sendgrid.Request;
import com.sendgrid.Response;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class EmailServiceTest {

    @Autowired
    private EmailService emailService;

    @MockBean  // ✅ Esta anotación debe estar sobre un campo, no sobre el método
    private SendGrid sendGrid;

    @Disabled("Test deshabilitado temporalmente")
    @Test
    public void testEnvioCorreo() throws Exception {
        // Configurar mock
        when(sendGrid.api(any(Request.class)))
                .thenReturn(new Response(202, "OK", null));

        // Ejecutar
        emailService.enviarCorreo("test@test.com", "Asunto", "Mensaje");

        // Verificar
        verify(sendGrid, times(1)).api(any(Request.class));
    }
}