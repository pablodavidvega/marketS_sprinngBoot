package com.example.MarketS.service;

import com.example.MarketS.dto.ReporteAdminDTO;
import com.example.MarketS.model.Categorias;
import com.example.MarketS.model.Marca;
import com.example.MarketS.model.Producto;
import com.example.MarketS.repository.CategoriaRepository;
import com.example.MarketS.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReporteAdminService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaRepository categoriasRepository;

    public ReporteAdminDTO generarReporte() {

        List<Producto> productos = productoRepository.findAll();
        List<Categorias> categorias = categoriasRepository.findAll();

        int totalProductos = productos.size();
        int totalCategorias = categorias.size();
        int totalMarcas = (int) productos.stream()
                .map(Producto::getMarca)
                .filter(java.util.Objects::nonNull)
                .map(Marca::getId)
                .distinct()
                .count();

        double promedioPrecio = productos.stream()
                .map(Producto::getPrecio)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double promedioStock = productos.stream()
                .map(Producto::getStock)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);

        // Nombres de categorías
        List<String> nombresCategorias = categorias.stream()
                .map(Categorias::getNombreCategoria)
                .collect(Collectors.toList());

        // Totales por categoría: una sola query GROUP BY en vez de una por categoría
        Map<Long, Long> conteoPorCategoria = productoRepository.countProductosGroupByCategoria().stream()
                .collect(Collectors.toMap(fila -> (Long) fila[0], fila -> (Long) fila[1]));
        List<Integer> totalesCategorias = categorias.stream()
                .map(c -> conteoPorCategoria.getOrDefault(c.getId(), 0L).intValue())
                .collect(Collectors.toList());

        // DTO
        ReporteAdminDTO dto = new ReporteAdminDTO();
        dto.setTotalProductos(totalProductos);
        dto.setTotalCategorias(totalCategorias);
        dto.setTotalMarcas(totalMarcas);
        dto.setPromedioPrecio(promedioPrecio);
        dto.setPromedioStock(promedioStock);
        dto.setNombresCategorias(nombresCategorias);
        dto.setTotalesCategorias(totalesCategorias);

        // Los gráficos se generaban acá como PNG en base64, pero ningún llamador los usa:
        // /reportes/catalogo los descarta explícitamente y /reportes/catalogo/excel tampoco
        // los lee. Se dejan vacíos para no gastar CPU/memoria en imágenes que nadie consume.
        dto.setGraficoCategoriasBase64("");
        dto.setGraficoPreciosStockBase64("");

        return dto;
    }
}
