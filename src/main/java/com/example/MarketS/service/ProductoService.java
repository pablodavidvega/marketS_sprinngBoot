package com.example.MarketS.service;

import com.example.MarketS.model.Producto;
import com.example.MarketS.model.TallaStock;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.TallaStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TallaStockRepository tallaStockRepository;

    // ==================== MÉTODOS EXISTENTES ====================

    public Page<Producto> buscarPorTodosLosCampos(String keyword, Long vendedorId, Pageable pageable) {
        return productoRepository.buscarPorVendedorYKeyword(vendedorId, keyword, pageable);
    }

    public Page<Producto> listarProductosPorVendedor(Long vendedorId, Pageable pageable) {
        return productoRepository.findByVendedorId(vendedorId, pageable);
    }

    // ==================== MÉTODOS NUEVOS PARA MANEJAR STOCK ====================

    /**
     * Reducir stock de un producto general (no por talla)
     */
    @Transactional
    public void reducirStock(Long productoId, Integer cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + productoId));

        // Verificar que haya suficiente stock
        if (producto.getStock() == null) {
            throw new RuntimeException("El producto no tiene stock definido");
        }

        if (producto.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre()
                    + ". Stock disponible: " + producto.getStock() + ", solicitado: " + cantidad);
        }

        // Reducir el stock
        producto.setStock(producto.getStock() - cantidad);

        // Si el stock llega a 0, cambiar estado a "No disponible"
        if (producto.getStock() == 0) {
            producto.setEstadoProducto("No disponible");
        }

        productoRepository.save(producto);

        System.out.println("✅ Stock reducido. Producto: " + producto.getNombre()
                + ", Nuevo stock: " + producto.getStock());
    }

    /**
     * Reducir stock de una talla específica (para ropa - categoría 3)
     */
    @Transactional
    public void reducirStockPorTalla(Long productoId, String talla, Integer cantidad) {
        // Buscar la talla específica
        TallaStock tallaStock = tallaStockRepository.findByProductoIdAndTalla(productoId, talla)
                .orElseThrow(() -> new RuntimeException("Talla " + talla + " no encontrada para el producto ID: " + productoId));

        // Verificar stock suficiente
        if (tallaStock.getStock() < cantidad) {
            throw new RuntimeException("Stock insuficiente para la talla " + talla
                    + ". Stock disponible: " + tallaStock.getStock() + ", solicitado: " + cantidad);
        }

        // Reducir stock de la talla
        tallaStock.setStock(tallaStock.getStock() - cantidad);
        tallaStockRepository.save(tallaStock);

        // Actualizar stock general del producto
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // Calcular nuevo stock general sumando todas las tallas
        Integer stockGeneral = tallaStockRepository.sumStockByProductoId(productoId);
        producto.setStock(stockGeneral);

        // Si stock general es 0, cambiar estado
        if (stockGeneral == 0) {
            producto.setEstadoProducto("No disponible");
        }

        productoRepository.save(producto);

        System.out.println("✅ Stock reducido por talla. Producto: " + producto.getNombre()
                + ", Talla: " + talla + ", Nuevo stock: " + tallaStock.getStock()
                + ", Stock general: " + stockGeneral);
    }

    /**
     * Verificar si un producto tiene suficiente stock
     */
    public boolean verificarStockSuficiente(Long productoId, Integer cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        return producto.getStock() != null && producto.getStock() >= cantidad;
    }

    /**
     * Verificar si una talla específica tiene suficiente stock
     */
    public boolean verificarStockSuficientePorTalla(Long productoId, String talla, Integer cantidad) {
        return tallaStockRepository.findByProductoIdAndTalla(productoId, talla)
                .map(t -> t.getStock() >= cantidad)
                .orElse(false);
    }

    /**
     * Obtener producto por ID
     */
    public Producto obtenerProductoPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));
    }

    /**
     * Actualizar producto con manejo especial para stock
     */
    @Transactional
    public void actualizarProductoConStock(Producto producto, Integer nuevoStock) {
        // Si se proporciona nuevo stock, actualizar
        if (nuevoStock != null && nuevoStock >= 0) {
            producto.setStock(nuevoStock);

            // Si stock es 0, cambiar estado
            if (nuevoStock == 0) {
                producto.setEstadoProducto("No disponible");
            } else if ("No disponible".equals(producto.getEstadoProducto())) {
                // Si había estado "No disponible" y ahora tiene stock, cambiar a "Disponible"
                producto.setEstadoProducto("Disponible");
            }
        }

        productoRepository.save(producto);
    }
}