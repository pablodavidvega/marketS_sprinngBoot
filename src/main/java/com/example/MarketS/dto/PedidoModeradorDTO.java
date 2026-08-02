package com.example.MarketS.dto;

import java.time.LocalDateTime;

// Proyección directa (sin materializar la entidad Pedido) para el listado de moderador.
// Pedido.detalles es FetchType.EAGER (y DetallePedido.producto también), así que traer la
// entidad completa dispara un N+1 aunque este listado no use detalles para nada — con esta
// query solo se seleccionan las 5 columnas que realmente se muestran.
public record PedidoModeradorDTO(Long id, String comprador, LocalDateTime fecha, String estado, Double total) {
}
