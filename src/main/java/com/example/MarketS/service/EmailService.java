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
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
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

    // Remitente con nombre visible ("MarketS <correo>") en vez de solo la dirección: ayuda al
    // puntaje de spam de Gmail/Outlook, junto con el Reply-To explícito de abajo.
    private String fromConNombre() {
        return "MarketS <" + mailFrom + ">";
    }

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Autowired
    private FrontendLinkBuilder frontendLinkBuilder;

    @Async
    public void enviarCorreo(String para, String asunto, String mensaje) {
        try {
            if (resendApiKey != null && !resendApiKey.isBlank()) {
                enviarViaResend(para, asunto, mensaje);
            } else {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(fromConNombre());
                mail.setReplyTo(mailFrom);
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
            sb.append("Ver en MarketS: ").append(frontendLinkBuilder.pedido(false, pedido.getId())).append("\n\n");
            sb.append("Gracias por tu compra en MarketS\n");

            if (resendApiKey != null && !resendApiKey.isBlank()) {
                enviarViaResend(para, asunto, sb.toString());
            } else {
                SimpleMailMessage mail = new SimpleMailMessage();
                mail.setFrom(fromConNombre());
                mail.setReplyTo(mailFrom);
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

    // Correo de bienvenida al completar el registro (normal o por Google) — con versión HTML
    // para que se vea bien en el cliente de correo, y texto plano como respaldo para el envío
    // por SMTP directo (dev, sin Resend) y para los clientes que no rendericen HTML.
    @Async
    public void enviarBienvenida(String para, String username, String rolname) {
        try {
            boolean esVendedor = "ROLE_VENDEDOR".equals(rolname);
            String asunto = (esVendedor ? "🚀 " : "🎉 ") + "¡Bienvenido/a a MarketS, " + username + "!";
            String destino = frontendLinkBuilder.bienvenida(rolname);
            String textoAlterno = textoBienvenida(username, esVendedor, destino);
            String html = htmlBienvenida(username, esVendedor, destino);

            if (resendApiKey != null && !resendApiKey.isBlank()) {
                enviarViaResend(para, asunto, textoAlterno, html);
            } else {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
                helper.setFrom(fromConNombre());
                helper.setReplyTo(mailFrom);
                helper.setTo(para);
                helper.setSubject(asunto);
                helper.setText(textoAlterno, html);
                mailSender.send(mime);
            }
            logger.info("Correo de bienvenida enviado a: {}", para);
        } catch (Exception e) {
            logger.error("Error enviando bienvenida a: {} - {}", para, e.getMessage());
        }
    }

    private String textoBienvenida(String username, boolean esVendedor, String destino) {
        StringBuilder sb = new StringBuilder();
        sb.append("¡Hola, ").append(username).append("!\n\n");
        sb.append("Bienvenido/a a MarketS. Tu cuenta ya está lista.\n\n");
        if (esVendedor) {
            sb.append("Para empezar a vender:\n");
            sb.append("  1. Publicá tu primer producto\n");
            sb.append("  2. Conectá tu cuenta de MercadoPago para cobrar directo: ")
                    .append(frontendLinkBuilder.conectarMercadoPago()).append("\n");
            sb.append("  3. Seguí tus ventas y pedidos desde tu panel\n\n");
        } else {
            sb.append("Para empezar:\n");
            sb.append("  1. Explorá el catálogo de vendedores\n");
            sb.append("  2. Guardá tus favoritos para no perderlos de vista\n");
            sb.append("  3. Seguí tus pedidos en tiempo real desde \"Mis pedidos\"\n\n");
        }
        sb.append("Entrá a tu cuenta: ").append(destino).append("\n\n");
        sb.append("¡Gracias por sumarte a MarketS!\n");
        return sb.toString();
    }

    private String htmlBienvenida(String username, boolean esVendedor, String destino) {
        String pasos = esVendedor
                ? """
                  <tr><td style="padding:6px 0;font-size:15px;color:#374151;">🏪&nbsp; Publicá tu primer producto</td></tr>
                  <tr><td style="padding:6px 0;font-size:15px;color:#374151;">💳&nbsp; Conectá <a href="%s" style="color:#2563eb;text-decoration:none;font-weight:600;">MercadoPago</a> para cobrar directo en tu cuenta</td></tr>
                  <tr><td style="padding:6px 0;font-size:15px;color:#374151;">📈&nbsp; Seguí tus ventas y pedidos desde tu panel</td></tr>
                  """.formatted(frontendLinkBuilder.conectarMercadoPago())
                : """
                  <tr><td style="padding:6px 0;font-size:15px;color:#374151;">🛍️&nbsp; Explorá el catálogo de vendedores</td></tr>
                  <tr><td style="padding:6px 0;font-size:15px;color:#374151;">❤️&nbsp; Guardá tus favoritos para no perderlos de vista</td></tr>
                  <tr><td style="padding:6px 0;font-size:15px;color:#374151;">📦&nbsp; Seguí tus pedidos en tiempo real desde "Mis pedidos"</td></tr>
                  """;

        String saludo = esVendedor ? "¡Tu tienda arranca acá, " + username + "! 🚀" : "¡Qué bueno tenerte, " + username + "! 🎉";
        String subtitulo = esVendedor
                ? "Tu cuenta de vendedor ya está lista. Estos son tus primeros pasos:"
                : "Tu cuenta ya está lista. Esto es lo que podés hacer:";
        String boton = esVendedor ? "Ir a mi panel de vendedor" : "Ir a MarketS";

        return """
                <!doctype html>
                <html lang="es">
                <body style="margin:0;padding:0;background-color:#f3f4f6;font-family:-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f3f4f6;padding:32px 16px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:480px;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 1px 3px rgba(0,0,0,0.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#2563eb,#1d4ed8);padding:32px 32px 28px;text-align:center;">
                              <div style="font-size:28px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">MarketS</div>
                              <div style="font-size:15px;color:#dbeafe;margin-top:4px;">Tu marketplace de siempre</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:32px;">
                              <h1 style="margin:0 0 8px;font-size:21px;color:#111827;">%s</h1>
                              <p style="margin:0 0 20px;font-size:15px;color:#6b7280;line-height:1.5;">%s</p>
                              <table role="presentation" cellpadding="0" cellspacing="0" style="width:100%%;background-color:#f9fafb;border-radius:12px;padding:16px 20px;margin-bottom:24px;">
                                %s
                              </table>
                              <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto;">
                                <tr>
                                  <td style="border-radius:10px;background-color:#2563eb;">
                                    <a href="%s" style="display:inline-block;padding:13px 28px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;">%s →</a>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px 28px;border-top:1px solid #f3f4f6;text-align:center;">
                              <p style="margin:0;font-size:12px;color:#9ca3af;">¿No creaste esta cuenta? Escribinos respondiendo este correo.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(saludo, subtitulo, pasos, destino, boton);
    }

    private void enviarViaResend(String to, String subject, String text) {
        enviarViaResend(to, subject, text, null);
    }

    private void enviarViaResend(String to, String subject, String text, String html) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resendApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>(Map.of(
            "from", fromConNombre(),
            "to", List.of(to),
            "reply_to", List.of(mailFrom),
            "subject", subject,
            "text", text
        ));
        if (html != null) {
            body.put("html", html);
        }

        String response = restTemplate.postForObject(
            "https://api.resend.com/emails",
            new HttpEntity<>(body, headers),
            String.class
        );
        logger.info("Resend response para {}: {}", to, response);
    }
}
