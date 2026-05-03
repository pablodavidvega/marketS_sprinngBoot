package com.example.MarketS.dto;

import com.example.MarketS.model.Pedido;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PedidoResponseDTO {

    private Long pedidoId;
    private String estado;
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
    }

    public static PedidoResponseDTO from(Pedido pedido) {
        return from(pedido, null, null);
    }

    public static PedidoResponseDTO from(Pedido pedido, String paymentUrl, String preferenceId) {
        List<DetalleDTO> detalles = pedido.getDetalles() != null
                ? pedido.getDetalles().stream().map(d -> DetalleDTO.builder()
                        .productoId(d.getProducto().getId())
                        .productoNombre(d.getProducto().getNombre())
                        .productoImagen(normalizeUrl(d.getProducto().getImagen()))
                        .precioUnitario(d.getPrecioUnitario())
                        .cantidad(d.getCantidad())
                        .subtotal(d.getPrecioUnitario() * d.getCantidad())
                        .build())
                        .toList()
                : List.of();

        return PedidoResponseDTO.builder()
                .pedidoId(pedido.getId())
                .estado(pedido.getEstado())
                .total(pedido.getTotal())
                .fecha(pedido.getFecha())
                .paymentUrl(paymentUrl)
                .preferenceId(preferenceId)
                .detalles(detalles)
                .build();
    }

    private static String normalizeUrl(String imagen) {
        if (imagen == null || imagen.isBlank()) return null;
        return imagen.startsWith("/uploads/") ? imagen : "/uploads/" + imagen;
    }
}
