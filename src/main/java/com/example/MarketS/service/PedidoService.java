package com.example.MarketS.service;
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

    private final PedidoRepository  pedidoRepository;
    private final ProductoRepository productoRepository;
    private final ProductoService productoService;

    public PedidoService(PedidoRepository pedidoRepository, ProductoRepository productoRepository,
                          ProductoService productoService) {
        this.pedidoRepository   = pedidoRepository;
        this.productoRepository = productoRepository;
        this.productoService    = productoService;
    }

    // Solo arma y persiste el pedido — no avisa a nadie. Avisar acá (como antes) significaba
    // notificar "pedido creado" apenas se guarda la fila, sin saber todavía si el pago va a
    // poder iniciarse (MercadoPago puede fallar al crear la preferencia justo después, ver
    // PedidoApiController.crearPedido) ni si es un acuerdo externo (que tiene su propio aviso
    // específico, ver PedidoApiController.crearPedidoConAcuerdoExterno). El llamador decide
    // cuándo avisar, una vez que sabe cómo terminó de confirmarse el pedido.
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

            // Reserva el stock atómicamente al crear el pedido en vez de solo validarlo contra
            // el valor en memoria: antes dos carritos concurrentes del mismo producto podían
            // pasar la validación y sobrevender, porque el descuento real recién ocurría al
            // confirmarse el pago (ver PedidoApiController.actualizarEstado). Si algo más abajo
            // en este método falla, la transacción de este método se revierte y el stock
            // reservado acá vuelve solo. Si el pedido se crea pero no llega a concretarse
            // (rechazado, expirado, o acuerdo externo no completado), quien lo resuelve así debe
            // restaurar el stock explícitamente — ver ProductoService.restaurarStock y sus usos
            // en PedidoApiController/PedidoExpiracionService.
            productoService.reducirStock(producto.getId(), cantidad);

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

        return saved;
    }
}
