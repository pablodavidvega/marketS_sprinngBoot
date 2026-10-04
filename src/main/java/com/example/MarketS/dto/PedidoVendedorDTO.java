package com.example.MarketS.dto;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
public class PedidoVendedorDTO {

    private static final Set<String> ESTADOS_CALIFICABLES = Set.of("RECIBIDO", "COMPLETADO_EXTERNO");

    private Long pedidoId;
    private Long vendedorId;
    private LocalDateTime fecha;
    private String estado;
    private String metodoPago;
    private String compradorUsername;
    private String compradorEmail;
    private String compradorPhone;
    private String compradorAddress;
    private ReputacionDTO compradorReputacion;
    private boolean puedeCalificarComprador;
    private boolean yaCalifiqueComprador;
    // Solo relevante cuando estado == ACORDADO_EXTERNO y el pedido tiene productos de más de un
    // vendedor: si este vendedor ya resolvió su parte (ver ResolucionVentaExterna) pero otro
    // vendedor del mismo pedido todavía no, el pedido sigue en ACORDADO_EXTERNO — este flag le
    // dice al frontend que oculte los botones "compra realizada/no se realizó" en esta vista.
    private boolean yaResolviMiParte;
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

    // Datos de contacto del comprador, resueltos en batch (ver
    // UserRepository.findContactInfoByIdIn) en vez de navegar pedido.getUsuario(), que
    // cargaría el User completo del comprador y dispararía una consulta extra por cada
    // comprador distinto (ver Pedido.usuario).
    public record CompradorInfo(String username, String email, String phone, String address) {}

    public static PedidoVendedorDTO from(Pedido pedido, User vendedor, Map<Long, CompradorInfo> compradores,
                                          Map<Long, ReputacionDTO> reputaciones, Set<Long> pedidosCalificados,
                                          Set<Long> pedidosResueltosPorMi) {
        Long compradorId = pedido.getUsuario().getId(); // seguro: id de un proxy LAZY, sin consulta
        CompradorInfo comprador = compradores.get(compradorId);

        List<ItemDTO> items = pedido.getDetalles().stream()
                .filter(d -> d.getProducto().getVendedor().getId().equals(vendedor.getId()))
                .map(d -> ItemDTO.builder()
                        .productoId(d.getProducto().getId())
                        .productoNombre(d.getProducto().getNombre())
                        .productoImagen(normalizeUrl(d.getProducto().getImagenThumb() != null ? d.getProducto().getImagenThumb() : d.getProducto().getImagen()))
                        .cantidad(d.getCantidad())
                        .precioUnitario(d.getPrecioUnitario())
                        .gananciaVendedor(d.getGananciaVendedor())
                        .build())
                .toList();

        boolean yaCalifique = pedidosCalificados.contains(pedido.getId());
        boolean puedeCalificar = !yaCalifique && ESTADOS_CALIFICABLES.contains(pedido.getEstado());

        return PedidoVendedorDTO.builder()
                .pedidoId(pedido.getId())
                .vendedorId(vendedor.getId())
                .fecha(pedido.getFecha())
                .estado(pedido.getEstado())
                .metodoPago(pedido.getMetodoPago())
                .compradorUsername(comprador != null ? comprador.username() : null)
                .compradorEmail(comprador != null ? comprador.email() : null)
                .compradorPhone(comprador != null ? comprador.phone() : null)
                .compradorAddress(comprador != null ? comprador.address() : null)
                .compradorReputacion(reputaciones.get(compradorId))
                .puedeCalificarComprador(puedeCalificar)
                .yaCalifiqueComprador(yaCalifique)
                .yaResolviMiParte(pedidosResueltosPorMi.contains(pedido.getId()))
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
