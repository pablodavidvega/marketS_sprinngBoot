package com.example.MarketS.service;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

// Un pedido puede quedar "atascado" esperando un pago que nunca se completa: el comprador
// olvida terminar el checkout de MercadoPago, o MercadoPago deja el pago en confirmación (pago
// en efectivo/transferencia) y nunca llega la confirmación final. Sin este job esos pedidos
// quedaban en PENDIENTE/PROCESANDO_PAGO/PENDIENTE_PAGO para siempre, mostrando al comprador un
// estado "en proceso" que en la práctica ya no iba a resolverse solo.
//
// Cada estado tiene su propio plazo de gracia (ver Pedido.fecha, que se fija al crear el pedido
// y no cambia después): PENDIENTE es un estado fantasma casi instantáneo (solo persiste si el
// servidor se cae a mitad de PedidoApiController.crearPedido), así que su plazo es corto; los
// otros dos reflejan cuánto tarda normalmente MercadoPago en confirmar cada método de pago.
@Service
@RequiredArgsConstructor
public class PedidoExpiracionService {

    private static final Logger logger = LoggerFactory.getLogger(PedidoExpiracionService.class);

    private final PedidoRepository pedidoRepository;
    private final UserRepository userRepository;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final FrontendLinkBuilder frontendLinkBuilder;
    private final ProductoService productoService;

    @Scheduled(cron = "0 0 * * * *")
    public void expirarPedidosSinResolver() {
        ejecutarSeguro("PENDIENTE huérfano", () -> expirar("PENDIENTE", Duration.ofHours(1)));
        ejecutarSeguro("PROCESANDO_PAGO sin completar", () -> expirar("PROCESANDO_PAGO", Duration.ofHours(24)));
        ejecutarSeguro("PENDIENTE_PAGO sin confirmar", () -> expirar("PENDIENTE_PAGO", Duration.ofHours(72)));
    }

    private void ejecutarSeguro(String nombre, Runnable tarea) {
        try {
            tarea.run();
        } catch (Exception e) {
            logger.error("Error expirando pedidos en '{}': {}", nombre, e.getMessage(), e);
        }
    }

    @Transactional
    void expirar(String estado, Duration plazoDeGracia) {
        LocalDateTime limite = LocalDateTime.now().minus(plazoDeGracia);
        List<Pedido> vencidos = pedidoRepository.findByFechaBeforeAndEstadoIn(limite, List.of(estado));

        for (Pedido pedido : vencidos) {
            pedido.setEstado("RECHAZADO");
            pedidoRepository.save(pedido);
            // El stock del pedido se reservó (descontó) al crearlo — ver PedidoService.crearPedido —
            // así que al expirar sin resolverse hay que restaurarlo para que vuelva a estar
            // disponible.
            pedido.getDetalles().forEach(d -> productoService.restaurarStock(d.getProducto().getId(), d.getCantidad()));
            notificarExpiracion(pedido);
        }
    }

    private void notificarExpiracion(Pedido pedido) {
        Long id = pedido.getId();
        userRepository.findById(pedido.getUsuario().getId()).ifPresent(comprador -> {
            notificacionService.crear(comprador,
                    "No se completó el pago del pedido #" + id + " a tiempo, así que se canceló. ¿Todavía te interesa? Podés intentarlo de nuevo desde tu carrito.",
                    "PEDIDO_PAGO_EXPIRADO", id);
            if (comprador.getEmail() != null && !comprador.getEmail().isBlank()) {
                emailService.enviarCorreo(comprador.getEmail(), "Tu pedido #" + id + " se canceló - MarketS",
                        "No se completó el pago del pedido #" + id + " dentro del plazo, así que se canceló."
                                + " Si todavía te interesa, podés intentarlo de nuevo desde tu carrito."
                                + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(false, id));
            }
        });
    }
}
