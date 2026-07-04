package com.example.MarketS.dto;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PedidoVendedorDTO {

    private Long pedidoId;
    private Long vendedorId;
    private LocalDateTime fecha;
    private String estado;
    private String metodoPago;
    private String compradorUsername;
    private String compradorEmail;
    private String compradorPhone;
    private String compradorAddress;
    private List<ItemDTO> items;

    @Getter
    @Builder
    public static class ItemDTO {
        private Long productoId;
        private String productoNombre;
        private String productoImagen;
        private Integer cantidad;
        private Double precioUnitario;
        private Double gananciaVendedor;
    }

    public static PedidoVendedorDTO from(Pedido pedido, User vendedor) {
        User comprador = pedido.getUsuario();
        List<ItemDTO> items = pedido.getDetalles().stream()
                .filter(d -> d.getProducto().getVendedor().getId().equals(vendedor.getId()))
                .map(d -> ItemDTO.builder()
                        .productoId(d.getProducto().getId())
                        .productoNombre(d.getProducto().getNombre())
                        .productoImagen(normalizeUrl(d.getProducto().getImagen()))
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .gananciaVendedor(d.getGananciaVendedor())
                        .build())
                .toList();

        return PedidoVendedorDTO.builder()
                .pedidoId(pedido.getId())
                .vendedorId(vendedor.getId())
                .fecha(pedido.getFecha())
                .estado(pedido.getEstado())
                .metodoPago(pedido.getMetodoPago())
                .compradorUsername(comprador.getUsername())
                .compradorEmail(comprador.getEmail())
                .compradorPhone(comprador.getPhone())
                .compradorAddress(comprador.getAddress())
                .items(items)
                .build();
    }

    private static String normalizeUrl(String imagen) {
        if (imagen == null || imagen.isBlank()) return null;
        String s = imagen.startsWith("/uploads/") ? imagen.substring(9) : imagen;
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return "/uploads/" + s;
    }
}
