package com.example.MarketS.service;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.MarketS.exception.RecursoNoEncontradoException;
import com.example.MarketS.exception.SolicitudInvalidaException;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ProductoRepository;

@Service
@Transactional
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);
    private static final double COMISION_APP = 0.0;

    private final PedidoRepository     pedidoRepository;
    private final ProductoRepository    productoRepository;
    private final EmailService          emailService;
    private final NotificacionService   notificacionService;
    private final FrontendLinkBuilder   frontendLinkBuilder;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         EmailService emailService,
                         NotificacionService notificacionService,
                         FrontendLinkBuilder frontendLinkBuilder) {
        this.pedidoRepository     = pedidoRepository;
        this.productoRepository   = productoRepository;
        this.emailService         = emailService;
        this.notificacionService  = notificacionService;
        this.frontendLinkBuilder  = frontendLinkBuilder;
    }

    public Pedido crearPedido(User usuario, Map<Integer, Integer> carrito) {
        log.debug("crearPedido: usuario={}, items={}", usuario.getId(), carrito.size());

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setEstado("PENDIENTE");
        pedido.setFecha(LocalDateTime.now());

        // Batch: una sola query para todos los productos del carrito en vez de un findById
        // por item (N+1) — con un carrito de 10 items eran 10 idas y vueltas a la BD.
        List<Long> productoIds = carrito.keySet().stream().map(Integer::longValue).toList();
        Map<Long, Producto> productosPorId = productoRepository.findAllById(productoIds).stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        double total = 0;
        List<DetallePedido> detalles = new ArrayList<>();

        // 3. Crear detalles
        for (Map.Entry<Integer, Integer> item : carrito.entrySet()) {
            long productoId = item.getKey().longValue();
            Producto producto = productosPorId.get(productoId);
            if (producto == null) {
                throw new RecursoNoEncontradoException("Producto no encontrado: " + productoId);
            }

            int cantidad = item.getValue();
            double precio = producto.getPrecio();

            int stockDisponible = producto.getStock() != null ? producto.getStock() : 0;
            if (cantidad > stockDisponible) {
                throw new SolicitudInvalidaException("Stock insuficiente para \"" + producto.getNombre()
                        + "\". Disponible: " + stockDisponible + ", solicitado: " + cantidad);
            }

            double subtotal = cantidad * precio;
            double comision = Math.round(subtotal * COMISION_APP * 100.0) / 100.0;
            double ganancia = Math.round((subtotal - comision) * 100.0) / 100.0;

            DetallePedido detalle = new DetallePedido();
            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecioUnitario(precio);
            detalle.setComisionApp(comision);
            detalle.setGananciaVendedor(ganancia);

            detalles.add(detalle);
            total += subtotal;
        }

        // Un pedido solo puede ser de un vendedor: lo exige el pago directo a la cuenta de
        // MercadoPago de ese vendedor (una preferencia de pago pertenece a un solo vendedor).
        // El carrito del frontend ya agrupa por vendedor antes de llamar a este endpoint,
        // pero se valida también acá por si se llama directo a la API.
        long vendedoresDistintos = detalles.stream()
                .map(d -> d.getProducto().getVendedor())
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .distinct()
                .count();
        if (vendedoresDistintos > 1) {
            throw new SolicitudInvalidaException("Todos los productos de un pedido deben ser del mismo vendedor");
        }

        // Asegurar que la lista de detalles no sea null
        pedido.setDetalles(detalles);
        pedido.setTotal(total);

        // 4. Guardar
        Pedido saved = pedidoRepository.save(pedido);
        log.info("Pedido {} creado para usuario {} por ${}", saved.getId(), usuario.getId(), total);

        // Confirmación al comprador: correo + notificación in-app (antes solo mandaba correo)
        String emailUsuario = saved.getUsuario().getEmail();
        if (emailUsuario != null && !emailUsuario.isBlank()) {
            emailService.enviarConfirmacionPedido(emailUsuario, saved);
        }
        notificacionService.crear(
                saved.getUsuario(),
                "Tu pedido #" + saved.getId() + " fue creado. Total: $" + total,
                "PEDIDO_CREADO",
                saved.getId()
        );

        // Notificar a cada vendedor afectado: notificación in-app + correo (antes el correo
        // solo se mandaba si el pago era externo, ver PedidoApiController.crearPedidoConAcuerdoExterno)
        String comprador = saved.getUsuario().getUsername();
        detalles.stream()
            .filter(d -> d.getProducto().getVendedor() != null)
            .collect(Collectors.groupingBy(d -> d.getProducto().getVendedor()))
            .forEach((vendedor, items) -> {
                String productos = items.stream()
                    .map(d -> d.getProducto().getNombre() + " (" + d.getCantidad() + " ud.)")
                    .collect(Collectors.joining(", "));
                notificacionService.crear(
                    vendedor,
                    comprador + " ordenó: " + productos,
                    "NUEVO_PEDIDO",
                    saved.getId()
                );
                if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
                    emailService.enviarCorreo(
                            vendedor.getEmail(),
                            "Nuevo pedido #" + saved.getId() + " - MarketS",
                            comprador + " ordenó: " + productos
                                    + "\n\nVer en MarketS: " + frontendLinkBuilder.pedido(true, saved.getId())
                    );
                }
            });

        return saved;
    }
}
