package com.example.MarketS.dto;

import java.util.List;

// Embudo de adopción de la comunidad, para el panel de administrador. No es un único funnel
// lineal: "publicaron" y "compraron" son dos tramos independientes que parten de "registrados"
// (un cliente nunca publica, un vendedor también puede comprar). Ver
// MARKETS-VISION-Y-FUNCIONALIDADES.md tarea #3.
public record EmbudoAdminDTO(
        long totalRegistrados,
        long totalVendedores,
        long totalPublicaron,
        long totalCompraron,
        long pedidosElegibles,
        long pedidosAbandonados,
        Double tasaAbandonoCheckout,
        Double diasPromedioRegistroAPublicacion,
        List<PuntoSemana> registrosPorSemana
) {
    public record PuntoSemana(String semana, long total) {}
}
