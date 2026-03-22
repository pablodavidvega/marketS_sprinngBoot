package com.example.MarketS.service;

import com.sendgrid.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.sendgrid.SendGrid;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.Method;

import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Content;
@Service
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String apiKey;

    @Value("${spring.mail.username:no-reply@tudominio.com}")
    private String emailAccount;

    public void enviarCorreo(String para, String asunto, String mensaje) {

        System.out.println("🔵 Enviando con SendGrid a: " + para);

        try {
            Email from = new Email(emailAccount);
            Email to = new Email(para);
            Content content = new Content("text/plain", mensaje);

            Mail mail = new Mail(from, asunto, to, content);

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            System.out.println("✅ Código respuesta: " + response.getStatusCode());

        } catch (Exception e) {
            System.out.println("❌ Error enviando con SendGrid");
            e.printStackTrace();
        }
    }
}