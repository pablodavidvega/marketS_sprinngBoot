package com.example.MarketS.dto;

import com.example.MarketS.model.Producto;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.Hibernate;

import java.util.Date;
import java.util.List;

@Getter
@Builder
public class ProductoResponseDTO {

    private Long id;
    private String nombre;
    private Double precio;
    private String descripcion;
    private String imagen;
    private String imagen2;
    private String imagen3;
    private String video;
    private Integer stock;
    private String estadoProducto;
    private boolean activo;
    private Date fechaAgregacion;
    private Long categoriaId;
    private String categoriaNombre;
    private Long marcaId;
    private String marcaNombre;
    private List<TallasStockDTO> tallas;
    private Double promedioCalificacion;
    private Long totalResenas;
    private Long totalVendido;

    public static ProductoResponseDTO from(Producto p) {
        return from(p, null, null, null);
    }

    public static ProductoResponseDTO from(Producto p, Double promedioCalificacion, Long totalResenas, Long totalVendido) {
        return ProductoResponseDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .precio(p.getPrecio())
                .descripcion(p.getDescripcion())
                .imagen(normalizeUrl(p.getImagen()))
                .imagen2(normalizeUrl(p.getImagen2()))
                .imagen3(normalizeUrl(p.getImagen3()))
                .video(p.getVideo())
                .stock(p.getStock())
                .estadoProducto(p.getEstadoProducto())
                .activo(p.getActivo())
                .fechaAgregacion(p.getFechaAgregacion())
                .categoriaId(p.getCategorias() != null ? p.getCategorias().getId() : null)
                .categoriaNombre(p.getCategorias() != null ? p.getCategorias().getNombreCategoria() : null)
                .marcaId(p.getMarca() != null ? p.getMarca().getId() : null)
                .marcaNombre(p.getMarca() != null ? p.getMarca().getNombreMarca() : null)
                .tallas(Hibernate.isInitialized(p.getTallas())
                        ? p.getTallas().stream()
                            .map(t -> new TallasStockDTO(t.getTalla(), t.getStock()))
                            .toList()
                        : List.of())
                .promedioCalificacion(promedioCalificacion)
                .totalResenas(totalResenas)
                .totalVendido(totalVendido)
                .build();
    }

    private static String normalizeUrl(String imagen) {
        if (imagen == null || imagen.isBlank()) return null;
        // Si ya es una URL absoluta (Supabase, CDN, etc.) la devolvemos tal cual
        if (imagen.startsWith("http://") || imagen.startsWith("https://")) return imagen;
        // Si ya tiene el prefijo local, no duplicar
        if (imagen.startsWith("/uploads/")) return imagen;
        return "/uploads/" + imagen;
    }
}
