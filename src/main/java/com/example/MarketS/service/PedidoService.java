package com.example.MarketS.service;
import com.example.MarketS.service.EmailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ProductoRepository;

@Service
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final EmailService emailService;

    public PedidoService(PedidoRepository pedidoRepository,
                         ProductoRepository productoRepository,
                         EmailService emailService) {
        this.pedidoRepository = pedidoRepository;
        this.productoRepository = productoRepository;
        this.emailService = emailService;
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

                DetallePedido detalle = new DetallePedido();
                detalle.setPedido(pedido);
                detalle.setProducto(producto);
                detalle.setCantidad(cantidad);
                detalle.setPrecioUnitario(precio);

                detalles.add(detalle);
                total += cantidad * precio;
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

            // Enviar correo de confirmación
            String emailUsuario = saved.getUsuario().getEmail();
            if (emailUsuario != null && !emailUsuario.isBlank()) {
                emailService.enviarConfirmacionPedido(emailUsuario, saved);
            }

            return saved;

        } catch (Exception e) {
            System.out.println("❌ EXCEPCIÓN en crearPedido: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
