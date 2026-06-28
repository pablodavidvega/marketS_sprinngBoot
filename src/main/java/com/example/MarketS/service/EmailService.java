package com.example.MarketS.service;

import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Async
    public void enviarCorreo(String para, String asunto, String mensaje) {
        try {
            if (resendApiKey != null && !resendApiKey.isBlank()) {
                enviarViaResend(para, asunto, mensaje);
            } else {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(mailFrom);
                mail.setTo(para);
                mail.setSubject(asunto);
                mail.setText(mensaje);
                mailSender.send(mail);
            }
            logger.info("Correo enviado correctamente a: {}", para);
        } catch (Exception e) {
            logger.error("Error al enviar correo a: {} - {}", para, e.getMessage());
        }
    }

    @Async
    public void enviarConfirmacionPedido(String para, Pedido pedido) {
        try {
            String asunto = "Confirmación de tu pedido #" + pedido.getId() + " - MarketS";

            StringBuilder sb = new StringBuilder();
            sb.append("¡Hola, ").append(pedido.getUsuario().getUsername()).append("!\n\n");
            sb.append("Tu pedido ha sido recibido exitosamente. Aquí está el resumen:\n\n");
            sb.append("Pedido #").append(pedido.getId()).append("\n");
            sb.append("Fecha: ").append(pedido.getFecha().toString()).append("\n");
            sb.append("Estado: ").append(pedido.getEstado()).append("\n\n");
            sb.append("─────────────────────────────\n");
            sb.append("PRODUCTOS:\n");

            for (DetallePedido detalle : pedido.getDetalles()) {
                sb.append("  • ").append(detalle.getProducto().getNombre())
                        .append(" x").append(detalle.getCantidad())
                        .append(" → $").append(String.format("%.2f", detalle.getPrecioUnitario() * detalle.getCantidad()))
                        .append("\n");
            }

            sb.append("─────────────────────────────\n");
            sb.append("TOTAL: $").append(String.format("%.2f", pedido.getTotal())).append("\n\n");
            sb.append("Gracias por tu compra en MarketS\n");

            if (resendApiKey != null && !resendApiKey.isBlank()) {
                enviarViaResend(para, asunto, sb.toString());
            } else {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(mailFrom);
                mail.setTo(para);
                mail.setSubject(asunto);
                mail.setText(sb.toString());
                mailSender.send(mail);
            }
            logger.info("Correo de confirmación enviado a: {}", para);
        } catch (Exception e) {
            logger.error("Error enviando confirmación a: {} - {}", para, e.getMessage());
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
}
