package com.example.MarketS.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Content;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String apiKey;

    @Value("${sendgrid.from.email:no-reply@tudominio.com}")
    private String fromEmail;

    public void enviarCorreo(String para, String asunto, String mensaje) {
        System.out.println("🔵 Enviando con SendGrid a: " + para);

        Email from = new Email(fromEmail);
        Email to = new Email(para);
        Content content = new Content("text/html", mensaje);  // text/html para formato
        Mail mail = new Mail(from, asunto, to, content);

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            System.out.println("✅ Código respuesta: " + response.getStatusCode());

            if (response.getStatusCode() != 202) {
                System.err.println("❌ Error: " + response.getBody());
            }

        } catch (IOException e) {
            System.err.println("❌ Error enviando con SendGrid: " + e.getMessage());
            e.printStackTrace();
        }
    }
}