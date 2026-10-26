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
import org.springframework.web.util.HtmlUtils;

import jakarta.mail.internet.MimeMessage;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

        return htmlCard(saludo, subtitulo, pasos, destino, boton,
                "¿No creaste esta cuenta? Escribinos respondiendo este correo.");
    }

    // Correo dedicado a la conexión de MercadoPago — se dispara en los 3 momentos en que un
    // usuario pasa a ser vendedor (registro directo, "Convertirme en vendedor" desde la
    // plataforma, o un admin le cambia el rol), no solo en el alta: sin esto, alguien que ya
    // tenía cuenta y se convierte en vendedor no se enteraba de que hace falta un paso extra
    // para poder cobrar (ver PedidoApiController: sin MercadoPago conectado, MarketS ni
    // siquiera deja pagarle por esa vía).
    @Async
    public void enviarGuiaMercadoPago(String para, String username) {
        try {
            String asunto = "💳 Un paso más para poder cobrar tus ventas - MarketS";
            String destino = frontendLinkBuilder.conectarMercadoPago();
            String textoAlterno = textoGuiaMercadoPago(username, destino);
            String html = htmlGuiaMercadoPago(username, destino);

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
            logger.info("Correo de guía MercadoPago enviado a: {}", para);
        } catch (Exception e) {
            logger.error("Error enviando guía MercadoPago a: {} - {}", para, e.getMessage());
        }
    }

    private String textoGuiaMercadoPago(String username, String destino) {
        StringBuilder sb = new StringBuilder();
        sb.append("¡Hola, ").append(username).append("!\n\n");
        sb.append("Ya sos vendedor en MarketS. Para poder cobrar tus ventas, conectá tu cuenta de MercadoPago:\n\n");
        sb.append("  1. Entrá a tu panel de vendedor → Pagos\n");
        sb.append("  2. Hacé clic en \"Conectar con MercadoPago\"\n");
        sb.append("  3. Iniciá sesión con tu cuenta de MercadoPago (o creá una gratis si no tenés)\n");
        sb.append("  4. Autorizá a MarketS a procesar pagos en tu nombre\n");
        sb.append("  5. Listo — vas a ver el estado en \"Conectado\"\n\n");
        sb.append("Conectar MercadoPago: ").append(destino).append("\n\n");
        sb.append("Mientras no la conectes, tus compradores solo van a poder pagarte por acuerdos externos ")
          .append("(efectivo, transferencia, etc.), no por MercadoPago.\n");
        return sb.toString();
    }

    private String htmlGuiaMercadoPago(String username, String destino) {
        String pasos = """
                <tr><td style="padding:7px 0;font-size:15px;color:#374151;"><b style="color:#2563eb;">1.</b>&nbsp; Entrá a tu panel de vendedor → <b>Pagos</b></td></tr>
                <tr><td style="padding:7px 0;font-size:15px;color:#374151;"><b style="color:#2563eb;">2.</b>&nbsp; Hacé clic en <b>"Conectar con MercadoPago"</b></td></tr>
                <tr><td style="padding:7px 0;font-size:15px;color:#374151;"><b style="color:#2563eb;">3.</b>&nbsp; Iniciá sesión con tu cuenta de MercadoPago (o creá una gratis si no tenés)</td></tr>
                <tr><td style="padding:7px 0;font-size:15px;color:#374151;"><b style="color:#2563eb;">4.</b>&nbsp; Autorizá a MarketS a procesar pagos en tu nombre</td></tr>
                <tr><td style="padding:7px 0;font-size:15px;color:#374151;"><b style="color:#2563eb;">5.</b>&nbsp; Listo — vas a ver el estado en <b>"Conectado"</b> ✅</td></tr>
                """;

        String saludo = "¡Un paso más, " + username + "! 💳";
        String subtitulo = "Ya sos vendedor en MarketS. Para poder cobrar tus ventas, conectá tu cuenta de MercadoPago:";
        String nota = "Mientras no la conectes, tus compradores solo van a poder pagarte por acuerdos externos "
                + "(efectivo, transferencia, etc.) — MarketS todavía no va a poder cobrarte por MercadoPago.";

        return htmlCard(saludo, subtitulo, pasos, destino, "Conectar MercadoPago", nota);
    }

    // Un producto dentro del resumen "Novedades del día" (ver NovedadesDiariasService)
    public record ProductoNovedad(String nombre, Double precio, String imagenUrl, String link) {}

    // Resumen "Novedades del día". Síncrono a propósito (sin @Async): NovedadesDiariasService
    // lo llama en serie, una persona a la vez, para respetar el límite de envíos por segundo
    // de Resend. Devuelve si el envío salió bien para poder contarlos.
    // linkBaja abre la página de confirmación del frontend; linkBajaUnClick es el endpoint al
    // que Gmail/Outlook hacen POST desde su botón "Anular suscripción" (List-Unsubscribe).
    public boolean enviarNovedades(String para, String username, List<ProductoNovedad> productos,
                                   int totalNuevos, String linkVerTodos, String linkBaja,
                                   String linkBajaUnClick) {
        try {
            String asunto = totalNuevos == 1
                    ? "🛍️ Hay 1 producto nuevo en MarketS"
                    : "🛍️ Hay " + totalNuevos + " productos nuevos en MarketS";
            String textoAlterno = textoNovedades(username, productos, totalNuevos, linkVerTodos, linkBaja);
            String html = htmlNovedades(username, productos, totalNuevos, linkVerTodos, linkBaja);
            Map<String, String> headers = Map.of(
                    "List-Unsubscribe", "<" + linkBajaUnClick + ">",
                    "List-Unsubscribe-Post", "List-Unsubscribe=One-Click");

            if (resendApiKey != null && !resendApiKey.isBlank()) {
                enviarViaResend(para, asunto, textoAlterno, html, headers);
            } else {
                MimeMessage mime = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
                helper.setFrom(fromConNombre());
                helper.setReplyTo(mailFrom);
                helper.setTo(para);
                helper.setSubject(asunto);
                helper.setText(textoAlterno, html);
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    mime.setHeader(h.getKey(), h.getValue());
                }
                mailSender.send(mime);
            }
            logger.info("Resumen de novedades enviado a: {}", para);
            return true;
        } catch (Exception e) {
            logger.error("Error enviando resumen de novedades a: {} - {}", para, e.getMessage());
            return false;
        }
    }

    private static String formatoPrecio(Double precio) {
        if (precio == null) return "";
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));
        nf.setMaximumFractionDigits(0);
        return "$" + nf.format(precio);
    }

    private String textoNovedades(String username, List<ProductoNovedad> productos, int totalNuevos,
                                  String linkVerTodos, String linkBaja) {
        StringBuilder sb = new StringBuilder();
        sb.append("¡Hola, ").append(username).append("!\n\n");
        sb.append("Esto es lo nuevo en MarketS:\n\n");
        for (ProductoNovedad p : productos) {
            sb.append("  • ").append(p.nombre()).append(" — ").append(formatoPrecio(p.precio()))
              .append("\n    ").append(p.link()).append("\n");
        }
        if (totalNuevos > productos.size()) {
            sb.append("\n...y ").append(totalNuevos - productos.size()).append(" más.\n");
        }
        sb.append("\nVer todos los productos nuevos: ").append(linkVerTodos).append("\n\n");
        sb.append("¿No querés recibir este resumen? Date de baja acá: ").append(linkBaja).append("\n");
        return sb.toString();
    }

    private String htmlNovedades(String username, List<ProductoNovedad> productos, int totalNuevos,
                                 String linkVerTodos, String linkBaja) {
        // Nombre de producto y usuario los escriben los usuarios: se escapan para que no puedan
        // inyectar HTML en el correo que reciben los demás.
        StringBuilder filas = new StringBuilder();
        for (ProductoNovedad p : productos) {
            String imagen = p.imagenUrl() != null
                    ? "<img src=\"%s\" width=\"56\" height=\"56\" alt=\"\" style=\"display:block;width:56px;height:56px;object-fit:cover;border-radius:8px;\">"
                        .formatted(HtmlUtils.htmlEscape(p.imagenUrl()))
                    : "<div style=\"width:56px;height:56px;border-radius:8px;background-color:#e5e7eb;\"></div>";
            filas.append("""
                    <tr><td style="padding:8px 0;">
                      <a href="%s" style="text-decoration:none;color:inherit;">
                        <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                          <td style="padding-right:14px;">%s</td>
                          <td>
                            <div style="font-size:15px;font-weight:600;color:#111827;">%s</div>
                            <div style="font-size:14px;color:#2563eb;margin-top:2px;">%s</div>
                          </td>
                        </tr></table>
                      </a>
                    </td></tr>
                    """.formatted(HtmlUtils.htmlEscape(p.link()), imagen,
                    HtmlUtils.htmlEscape(p.nombre()), formatoPrecio(p.precio())));
        }
        if (totalNuevos > productos.size()) {
            filas.append("<tr><td style=\"padding:8px 0 0;font-size:14px;color:#6b7280;\">...y %d más</td></tr>"
                    .formatted(totalNuevos - productos.size()));
        }

        String titulo = "¡Hola, " + HtmlUtils.htmlEscape(username) + "! 🛍️";
        String subtitulo = totalNuevos == 1
                ? "Se publicó 1 producto nuevo en MarketS:"
                : "Se publicaron " + totalNuevos + " productos nuevos en MarketS:";
        String nota = "Recibís este resumen porque estás suscrito/a a las novedades de MarketS. "
                + "<a href=\"" + HtmlUtils.htmlEscape(linkBaja) + "\" style=\"color:#9ca3af;\">No quiero recibir estos correos</a>";

        return htmlCard(titulo, subtitulo, filas.toString(), linkVerTodos, "Ver productos nuevos", nota);
    }

    // Estructura visual común a todos los correos "de tarjeta" (bienvenida, guía de
    // MercadoPago): header con la marca, título + bajada, bloque de pasos y botón CTA.
    private String htmlCard(String h1, String subtitulo, String bloqueInteriorFilasTabla,
                             String ctaHref, String ctaLabel, String notaPie) {
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
                              <p style="margin:0;font-size:12px;color:#9ca3af;">%s</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(h1, subtitulo, bloqueInteriorFilasTabla, ctaHref, ctaLabel, notaPie);
    }

    private void enviarViaResend(String to, String subject, String text) {
        enviarViaResend(to, subject, text, null);
    }

    private void enviarViaResend(String to, String subject, String text, String html) {
        enviarViaResend(to, subject, text, html, null);
    }

    private void enviarViaResend(String to, String subject, String text, String html,
                                 Map<String, String> headersCorreo) {
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
        if (headersCorreo != null) {
            body.put("headers", headersCorreo);
        }

        String response = restTemplate.postForObject(
            "https://api.resend.com/emails",
            new HttpEntity<>(body, headers),
            String.class
        );
        logger.info("Resend response para {}: {}", to, response);
    }
}
