package com.example.MarketS.service;

import com.example.MarketS.dto.ReputacionDTO;
import com.example.MarketS.repository.DetallePedidoRepository;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.repository.ResenaCompradorRepository;
import com.example.MarketS.repository.ResenaRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Calcula la reputación de un usuario (como vendedor o como comprador) combinando el promedio
// de calificaciones (estrellas) con la tasa de éxito de sus transacciones, estilo el
// "termómetro" de Mercado Libre. Ver MARKETS-VISION-Y-FUNCIONALIDADES.md tarea #2.
@Service
public class ReputacionService {

    // Con menos de este número de transacciones resueltas (exitosas + fallidas) se considera
    // que no hay historial suficiente para un score confiable — se muestra "Nuevo".
    private static final int MIN_TRANSACCIONES = 3;

    private static final List<String> ESTADOS_EXITOSOS = List.of("RECIBIDO", "COMPLETADO_EXTERNO");
    private static final List<String> ESTADOS_FALLIDOS = List.of("RECHAZADO_EXTERNO");

    private final ResenaRepository resenaRepository;
    private final ResenaCompradorRepository resenaCompradorRepository;
    private final DetallePedidoRepository detallePedidoRepository;
    private final PedidoRepository pedidoRepository;

    public ReputacionService(ResenaRepository resenaRepository,
                              ResenaCompradorRepository resenaCompradorRepository,
                              DetallePedidoRepository detallePedidoRepository,
                              PedidoRepository pedidoRepository) {
        this.resenaRepository = resenaRepository;
        this.resenaCompradorRepository = resenaCompradorRepository;
        this.detallePedidoRepository = detallePedidoRepository;
        this.pedidoRepository = pedidoRepository;
    }

    public ReputacionDTO calcularVendedor(Long vendedorId) {
        double avgEstrellas = resenaRepository.avgCalificacionByVendedorId(vendedorId);
        long totalCalificaciones = resenaRepository.countByProductoVendedorId(vendedorId);
        long exitosos = detallePedidoRepository.countPedidosDistinctByVendedorAndEstadoIn(vendedorId, ESTADOS_EXITOSOS);
        long fallidos = detallePedidoRepository.countPedidosDistinctByVendedorAndEstadoIn(vendedorId, ESTADOS_FALLIDOS);
        return calcular(avgEstrellas, totalCalificaciones, exitosos, fallidos);
    }

    public ReputacionDTO calcularComprador(Long compradorId) {
        double avgEstrellas = resenaCompradorRepository.avgCalificacionByCompradorId(compradorId);
        long totalCalificaciones = resenaCompradorRepository.countByCompradorId(compradorId);
        long exitosos = pedidoRepository.countByUsuarioIdAndEstadoIn(compradorId, ESTADOS_EXITOSOS);
        long fallidos = pedidoRepository.countByUsuarioIdAndEstadoIn(compradorId, ESTADOS_FALLIDOS);
        return calcular(avgEstrellas, totalCalificaciones, exitosos, fallidos);
    }

    // Batch: reputación de vendedores para una lista de ids en un número fijo de queries (3),
    // en vez de 4 por vendedor — evita el N+1 que hacía lento el panel de admin con muchos
    // usuarios (ver AdminApiController.distribucionReputacion).
    public Map<Long, ReputacionDTO> calcularVendedoresBatch(List<Long> vendedorIds) {
        if (vendedorIds.isEmpty()) return Map.of();

        Map<Long, double[]> resenas = new HashMap<>();
        resenaRepository.findEstadisticasAgrupadasPorVendedor().forEach(row -> {
            Long id = ((Number) row[0]).longValue();
            double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            long count = ((Number) row[2]).longValue();
            resenas.put(id, new double[]{ avg, count });
        });
        Map<Long, Long> exitosos = aMapa(detallePedidoRepository.countPedidosDistinctAgrupadoPorVendedor(ESTADOS_EXITOSOS));
        Map<Long, Long> fallidos = aMapa(detallePedidoRepository.countPedidosDistinctAgrupadoPorVendedor(ESTADOS_FALLIDOS));

        Map<Long, ReputacionDTO> resultado = new HashMap<>();
        for (Long id : vendedorIds) {
            double[] stats = resenas.getOrDefault(id, new double[]{ 0.0, 0 });
            resultado.put(id, calcular(stats[0], (long) stats[1], exitosos.getOrDefault(id, 0L), fallidos.getOrDefault(id, 0L)));
        }
        return resultado;
    }

    // Batch: mismo criterio que calcularVendedoresBatch pero para compradores.
    public Map<Long, ReputacionDTO> calcularCompradoresBatch(List<Long> compradorIds) {
        if (compradorIds.isEmpty()) return Map.of();

        Map<Long, double[]> resenas = new HashMap<>();
        resenaCompradorRepository.findEstadisticasAgrupadasPorComprador().forEach(row -> {
            Long id = ((Number) row[0]).longValue();
            double avg = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            long count = ((Number) row[2]).longValue();
            resenas.put(id, new double[]{ avg, count });
        });
        Map<Long, Long> exitosos = aMapa(pedidoRepository.countAgrupadoPorUsuarioYEstadoIn(ESTADOS_EXITOSOS));
        Map<Long, Long> fallidos = aMapa(pedidoRepository.countAgrupadoPorUsuarioYEstadoIn(ESTADOS_FALLIDOS));

        Map<Long, ReputacionDTO> resultado = new HashMap<>();
        for (Long id : compradorIds) {
            double[] stats = resenas.getOrDefault(id, new double[]{ 0.0, 0 });
            resultado.put(id, calcular(stats[0], (long) stats[1], exitosos.getOrDefault(id, 0L), fallidos.getOrDefault(id, 0L)));
        }
        return resultado;
    }

    private Map<Long, Long> aMapa(List<Object[]> filas) {
        Map<Long, Long> mapa = new HashMap<>();
        filas.forEach(fila -> mapa.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue()));
        return mapa;
    }

    private ReputacionDTO calcular(double avgEstrellas, long totalCalificaciones, long exitosos, long fallidos) {
        long resueltos = exitosos + fallidos;
        if (resueltos < MIN_TRANSACCIONES) {
            return ReputacionDTO.nuevo(totalCalificaciones > 0 ? avgEstrellas : null, totalCalificaciones);
        }

        Double normEstrellas = totalCalificaciones > 0 ? avgEstrellas / 5.0 : null;
        double tasaExito = (double) exitosos / resueltos;
        double fraccion = normEstrellas != null ? (normEstrellas * 0.5 + tasaExito * 0.5) : tasaExito;

        int score = (int) Math.round(fraccion * 100);
        String nivel = nivelDe(score);

        return new ReputacionDTO(
                "CALCULADA",
                score,
                nivel,
                totalCalificaciones > 0 ? avgEstrellas : null,
                totalCalificaciones,
                tasaExito,
                resueltos
        );
    }

    private String nivelDe(int score) {
        if (score < 20) return "BAJA";
        if (score < 40) return "REGULAR";
        if (score < 60) return "BUENA";
        if (score < 80) return "MUY_BUENA";
        return "EXCELENTE";
    }
}
