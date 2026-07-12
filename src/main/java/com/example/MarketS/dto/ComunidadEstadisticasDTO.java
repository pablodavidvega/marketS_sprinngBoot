package com.example.MarketS.dto;

import java.util.List;

// Uso y transacciones de la comunidad, pensado para justificar el pago B2B ante quien
// administra la comunidad (empresa/organización que paga la suscripción). Ver
// MARKETS-VISION-Y-FUNCIONALIDADES.md tarea #7.
public record ComunidadEstadisticasDTO(
        long totalRegistrados,
        long totalTransaccionaron,
        Double tasaAdopcion,
        long totalTransacciones,
        double gmvTotal,
        long totalFavoritos,
        long totalEncuentros,
        List<PuntoSemana> transaccionesPorSemana
) {
    public record PuntoSemana(String semana, long total, double monto) {}
}
