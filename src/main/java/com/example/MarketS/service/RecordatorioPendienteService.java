package com.example.MarketS.service;

import com.example.MarketS.model.ChatMensaje;
import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.Pregunta;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.ChatMensajeRepository;
import com.example.MarketS.repository.NotificacionRepository;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.PreguntaRepository;
import com.example.MarketS.repository.ResolucionVentaExternaRepository;
import com.example.MarketS.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Recordatorios diarios de "asuntos pendientes": a diferencia de una Notificacion normal (se
// crea una vez, se marca leída y queda cerrada para siempre), estos vuelven a generarse cada
// día mientras el asunto de fondo (no la notificación) siga sin resolverse. La resolución se
// detecta consultando el estado real de Pedido/Pregunta/ChatMensaje, no la tabla notificacion.
@Service
@RequiredArgsConstructor
public class RecordatorioPendienteService {

    private static final Logger logger = LoggerFactory.getLogger(RecordatorioPendienteService.class);

    // Para chat y pregunta (que sí tienen timestamp del evento), exigimos esta antigüedad
    // mínima antes del primer recordatorio, para no avisar el mismo día que se generó el
    // pendiente si el job corre poco después.
    private static final int HORAS_GRACIA_CHAT_PREGUNTA = 6;

    private final ChatMensajeRepository chatMensajeRepository;
    private final PreguntaRepository preguntaRepository;
    private final PedidoRepository pedidoRepository;
    private final ResolucionVentaExternaRepository resolucionVentaExternaRepository;
    private final NotificacionRepository notificacionRepository;
    private final UserRepository userRepository;
    private final NotificacionService notificacionService;

    @Scheduled(cron = "0 0 9 * * *")
    public void ejecutarRecordatoriosDiarios() {
        LocalDateTime inicioDeHoy = LocalDate.now().atStartOfDay();
        ejecutarSeguro("chat sin responder", () -> recordarChatSinResponder(inicioDeHoy));
        ejecutarSeguro("pregunta sin responder", () -> recordarPreguntasSinResponder(inicioDeHoy));
        ejecutarSeguro("compra externa sin confirmar", () -> recordarComprasExternasSinConfirmar(inicioDeHoy));
        ejecutarSeguro("confirmar recepción", () -> recordarConfirmarRecepcion(inicioDeHoy));
    }

    private void ejecutarSeguro(String nombre, Runnable tarea) {
        try {
            tarea.run();
        } catch (Exception e) {
            logger.error("Error generando recordatorios de '{}': {}", nombre, e.getMessage(), e);
        }
    }

    @Transactional
    void recordarChatSinResponder(LocalDateTime inicioDeHoy) {
        LocalDateTime limiteGracia = LocalDateTime.now().minusHours(HORAS_GRACIA_CHAT_PREGUNTA);

        for (ChatMensaje ultimo : chatMensajeRepository.findUltimoMensajePorHilo()) {
            if (ultimo.getFechaEnvio().isAfter(limiteGracia)) continue;

            Long pedidoId = ultimo.getPedido().getId();
            Long vendedorId = ultimo.getVendedor().getId();
            boolean autorEsVendedor = ultimo.getAutor().getId().equals(vendedorId);

            User destinatario = autorEsVendedor
                    ? ultimo.getPedido().getUsuario()
                    : userRepository.getReferenceById(vendedorId);
            Long destinatarioId = destinatario.getId();

            if (notificacionRepository.existeRecordatorioDesde(
                    destinatarioId, "RECORDATORIO_CHAT_SIN_RESPONDER", pedidoId, null, vendedorId, inicioDeHoy)) {
                continue;
            }

            notificacionService.crear(destinatario,
                    "Tenés un mensaje sin responder en el pedido #" + pedidoId,
                    "RECORDATORIO_CHAT_SIN_RESPONDER", pedidoId, null, vendedorId);
        }
    }

    @Transactional
    void recordarPreguntasSinResponder(LocalDateTime inicioDeHoy) {
        List<Pregunta> pendientes = preguntaRepository.findPendientesDeRespuesta();

        Map<Long, List<Pregunta>> porProducto = pendientes.stream()
                .collect(Collectors.groupingBy(p -> p.getProducto().getId()));

        for (Map.Entry<Long, List<Pregunta>> entry : porProducto.entrySet()) {
            Long productoId = entry.getKey();
            List<Pregunta> preguntasDelProducto = entry.getValue();
            Long vendedorId = preguntasDelProducto.get(0).getProducto().getVendedor() != null
                    ? preguntasDelProducto.get(0).getProducto().getVendedor().getId()
                    : null;
            if (vendedorId == null) continue;

            User destinatario = userRepository.getReferenceById(vendedorId);
            String nombreProducto = preguntasDelProducto.get(0).getProducto().getNombre();
            int cantidad = preguntasDelProducto.size();

            if (notificacionRepository.existeRecordatorioDesde(
                    vendedorId, "RECORDATORIO_PREGUNTA_SIN_RESPONDER", null, productoId, null, inicioDeHoy)) {
                continue;
            }

            String mensaje = cantidad == 1
                    ? "Tenés una pregunta sin responder en tu producto \"" + nombreProducto + "\""
                    : "Tenés " + cantidad + " preguntas sin responder en tu producto \"" + nombreProducto + "\"";
            notificacionService.crear(destinatario, mensaje, "RECORDATORIO_PREGUNTA_SIN_RESPONDER", null, productoId);
        }
    }

    @Transactional
    void recordarComprasExternasSinConfirmar(LocalDateTime inicioDeHoy) {
        List<Pedido> acordadosExterno = pedidoRepository.findByEstado("ACORDADO_EXTERNO");
        if (acordadosExterno.isEmpty()) return;

        List<Long> pedidoIds = acordadosExterno.stream().map(Pedido::getId).toList();
        Set<String> yaResueltos = resolucionVentaExternaRepository.findParesPedidoVendedorResueltos(pedidoIds).stream()
                .map(par -> par[0] + "_" + par[1])
                .collect(Collectors.toCollection(HashSet::new));

        for (Pedido pedido : acordadosExterno) {
            Long pedidoId = pedido.getId();
            Set<Long> vendedoresDelPedido = pedido.getDetalles().stream()
                    .map(DetallePedido::getProducto)
                    .map(producto -> producto.getVendedor())
                    .filter(java.util.Objects::nonNull)
                    .map(User::getId)
                    .collect(Collectors.toSet());

            for (Long vendedorId : vendedoresDelPedido) {
                if (yaResueltos.contains(pedidoId + "_" + vendedorId)) continue;

                if (notificacionRepository.existeRecordatorioDesde(
                        vendedorId, "RECORDATORIO_COMPRA_EXTERNA_SIN_CONFIRMAR", pedidoId, null, null, inicioDeHoy)) {
                    continue;
                }

                notificacionService.crear(userRepository.getReferenceById(vendedorId),
                        "Todavía no confirmaste si se concretó la venta externa del pedido #" + pedidoId,
                        "RECORDATORIO_COMPRA_EXTERNA_SIN_CONFIRMAR", pedidoId);
            }
        }
    }

    @Transactional
    void recordarConfirmarRecepcion(LocalDateTime inicioDeHoy) {
        for (Pedido pedido : pedidoRepository.findByEstado("PAGADO")) {
            Long pedidoId = pedido.getId();
            User comprador = pedido.getUsuario();
            Long compradorId = comprador.getId();

            if (notificacionRepository.existeRecordatorioDesde(
                    compradorId, "RECORDATORIO_CONFIRMAR_RECEPCION", pedidoId, null, null, inicioDeHoy)) {
                continue;
            }

            notificacionService.crear(comprador,
                    "Recordá confirmar que recibiste tu pedido #" + pedidoId,
                    "RECORDATORIO_CONFIRMAR_RECEPCION", pedidoId);
        }
    }
}
