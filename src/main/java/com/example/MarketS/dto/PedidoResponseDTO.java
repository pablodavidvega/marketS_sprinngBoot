package com.example.MarketS.dto;

import com.example.MarketS.model.Pedido;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
public class PedidoResponseDTO {

    private Long pedidoId;
    private String estado;
    private String metodoPago;
    private Double total;
    private LocalDateTime fecha;
    private String paymentUrl;
    private String preferenceId;
    private List<DetalleDTO> detalles;

    @Getter
    @Builder
    public static class DetalleDTO {
        private Long productoId;
        private String productoNombre;
        private String productoImagen;
        private Double precioUnitario;
        private Integer cantidad;
        private Double subtotal;
        private Long vendedorId;
        private String vendedorUsername;
        private Boolean yaReseno;
    }

    public static PedidoResponseDTO from(Pedido pedido) {
        return from(pedido, null, null);
    }

    public static PedidoResponseDTO from(Pedido pedido, String paymentUrl, String preferenceId) {
        return from(pedido, paymentUrl, preferenceId, null);
    }

    // Variante para listados (ver PedidoApiController.misPedidos / ClienteApiController.dashboard):
    // recibe los usernames de vendedores ya resueltos en batch en vez de tocar
    // d.getProducto().getVendedor().getUsername(), que cargaría el User completo y dispararía
    // una consulta extra por cada vendedor distinto (ver Producto.vendedor).
    public static PedidoResponseDTO from(Pedido pedido, String paymentUrl, String preferenceId,
                                          Map<Long, String> vendedorUsernames) {
        return from(pedido, paymentUrl, preferenceId, vendedorUsernames, null);
    }

    // Variante con estado de reseña por producto (ver PedidoApiController.misPedidos): permite
    // mostrar el botón "Calificar" en la vista de pedidos sin que el comprador tenga que volver
    // al detalle del producto — misma reseña de producto de siempre (Resena), un solo botón que
    // ya la alimenta.
    public static PedidoResponseDTO from(Pedido pedido, String paymentUrl, String preferenceId,
                                          Map<Long, String> vendedorUsernames, Set<Long> productosResenados) {
        List<DetalleDTO> detalles = pedido.getDetalles() != null
                ? pedido.getDetalles().stream().map(d -> {
                        Long vendedorId = d.getProducto().getVendedor() != null ? d.getProducto().getVendedor().getId() : null;
                        String vendedorUsername = vendedorUsernames != null && vendedorId != null
                                ? vendedorUsernames.get(vendedorId)
                                : (d.getProducto().getVendedor() != null ? d.getProducto().getVendedor().getUsername() : null);
                        return DetalleDTO.builder()
                                .productoId(d.getProducto().getId())
                                .productoNombre(d.getProducto().getNombre())
                                .productoImagen(normalizeUrl(d.getProducto().getImagenThumb() != null ? d.getProducto().getImagenThumb() : d.getProducto().getImagen()))
                                .precioUnitario(d.getPrecioUnitario())
                                .cantidad(d.getCantidad())
                                .subtotal(d.getPrecioUnitario() * d.getCantidad())
                                .vendedorId(vendedorId)
                                .vendedorUsername(vendedorUsername)
                                .yaReseno(productosResenados != null && productosResenados.contains(d.getProducto().getId()))
                                .build();
                    })
                        .toList()
                : List.of();

        return PedidoResponseDTO.builder()
                .pedidoId(pedido.getId())
                .estado(pedido.getEstado())
                .metodoPago(pedido.getMetodoPago())
                .total(pedido.getTotal())
                .fecha(pedido.getFecha())
                .paymentUrl(paymentUrl)
                .preferenceId(preferenceId)
                .detalles(detalles)
                .build();
    }

    private static String normalizeUrl(String imagen) {
        if (imagen == null || imagen.isBlank()) return null;
        String s = imagen.startsWith("/uploads/") ? imagen.substring(9) : imagen;
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return "/uploads/" + s;
    }
}
