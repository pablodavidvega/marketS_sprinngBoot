package com.example.MarketS.service;

import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.Pedido;
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

    @Async
    public void enviarConfirmacionPedido(String para, Pedido pedido) {
        try {
            String asunto = "Confirmación de tu pedido #" + pedido.getId() + " - MarketS";

            StringBuilder sb = new StringBuilder();
            sb.append("¡Hola, ").append(pedido.getUsuario().getUsername()).append("!\n\n");
            sb.append("Tu pedido ha sido recibido exitosamente. Aquí está el resumen:\n\n");
            sb.append("📦 Pedido #").append(pedido.getId()).append("\n");
            sb.append("📅 Fecha: ").append(pedido.getFecha().toString()).append("\n");
            sb.append("📌 Estado: ").append(pedido.getEstado()).append("\n\n");
            sb.append("─────────────────────────────\n");
            sb.append("PRODUCTOS:\n");

            for (DetallePedido detalle : pedido.getDetalles()) {
                sb.append("  • ").append(detalle.getProducto().getNombre())
                        .append(" x").append(detalle.getCantidad())
                        .append(" → $").append(String.format("%.2f", detalle.getPrecioUnitario() * detalle.getCantidad()))
                        .append("\n");
            }

            sb.append("─────────────────────────────\n");
            sb.append(" TOTAL: $").append(String.format("%.2f", pedido.getTotal())).append("\n\n");
            sb.append("Gracias por tu compra en MarketS 🛒\n");

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom("pablo.david.vega1@gmail.com");
            mail.setTo(para);
            mail.setSubject(asunto);
            mail.setText(sb.toString());

            mailSender.send(mail);
            System.out.println(" Correo de confirmación enviado a: " + para);

        } catch (Exception e) {
            System.out.println(" Error enviando confirmación a: " + para + " - " + e.getMessage());
        }
    }
}