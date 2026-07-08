package com.example.MarketS.dto;

// Reputación estilo "termómetro" de Mercado Libre: combina el promedio de calificaciones
// (estrellas) con la tasa de éxito de las transacciones (pedidos concretados vs. no concretados).
// estado = "NUEVO" cuando aún no hay suficiente historial (ver ReputacionService) — en ese caso
// score/nivel/tasaExito vienen en null y el frontend debe mostrar un badge "Nuevo" en vez de barra.
public record ReputacionDTO(
        String estado,
        Integer score,
        String nivel,
        Double promedioEstrellas,
        long totalCalificaciones,
        Double tasaExito,
        long transaccionesResueltas
) {
    public static ReputacionDTO nuevo(Double promedioEstrellas, long totalCalificaciones) {
        return new ReputacionDTO("NUEVO", null, null, promedioEstrellas, totalCalificaciones, null, 0);
    }
}
