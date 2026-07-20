package com.example.MarketS.service;
import com.example.MarketS.service.EmailService;
import com.example.MarketS.service.NotificacionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ProductoRepository;

@Service
@Transactional
public class PedidoService {

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
        System.out.println("\n=== 🛠️ SERVICE: crearPedido() ===");
        System.out.println("Usuario ID: " + usuario.getId());
        System.out.println("Items en carrito: " + carrito.size());

        try {
            // 1. Validar pedido pendiente - COMENTA ESTO TEMPORALMENTE
            // Optional<Pedido> pedidoExistente = pedidoRepository.findByUsuarioAndEstado(usuario, "PENDIENTE");
            // if (pedidoExistente.isPresent()) {
            //     System.out.println("⚠️ Ya existe pedido PENDIENTE, retornando existente");
            //     return pedidoExistente.get();
            // }

            // 2. Crear pedido
            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            pedido.setEstado("PENDIENTE");
            pedido.setFecha(LocalDateTime.now());

            System.out.println("Nuevo pedido creado");

            double total = 0;
            List<DetallePedido> detalles = new ArrayList<>();

            // 3. Crear detalles
            for (Map.Entry<Integer, Integer> item : carrito.entrySet()) {
                System.out.println("Procesando producto ID: " + item.getKey());

                Producto producto = productoRepository.findById(item.getKey().longValue())
                        .orElseThrow(() -> {
                            System.out.println("❌ Producto no encontrado ID: " + item.getKey());
                            return new RuntimeException("Producto no encontrado");
                        });

                int cantidad = item.getValue();
                double precio = producto.getPrecio();

                System.out.println("   - " + producto.getNombre() + ": " + cantidad + " x $" + precio);

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

            // Asegurar que la lista de detalles no sea null
            pedido.setDetalles(detalles);
            pedido.setTotal(total);

            System.out.println("Total calculado: $" + total);
            System.out.println("Número de detalles: " + detalles.size());

            // 4. Guardar
            System.out.println("Guardando en BD...");
            Pedido saved = pedidoRepository.save(pedido);
            System.out.println("✅ Pedido guardado ID: " + saved.getId());

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

        } catch (Exception e) {
            System.out.println("❌ EXCEPCIÓN en crearPedido: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
