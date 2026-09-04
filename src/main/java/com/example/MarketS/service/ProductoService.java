package com.example.MarketS.service;

import com.example.MarketS.exception.RecursoNoEncontradoException;
import com.example.MarketS.exception.SolicitudInvalidaException;
import com.example.MarketS.model.Producto;
import com.example.MarketS.model.TallaStock;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.ProductoRepository;
import com.example.MarketS.repository.TallaStockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    // Umbral de "stock bajo": por debajo de esto se avisa al vendedor. Se avisa solo al
    // cruzar el umbral hacia abajo (no en cada venta subsiguiente) para no saturarlo de avisos.
    private static final int UMBRAL_STOCK_BAJO = 10;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private TallaStockRepository tallaStockRepository;

    @Autowired
    private NotificacionService notificacionService;

    @Autowired
    private EmailService emailService;

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
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + productoId));

        // Verificar que haya suficiente stock
        if (producto.getStock() == null) {
            throw new SolicitudInvalidaException("El producto no tiene stock definido");
        }

        // UPDATE atómico condicionado (WHERE stock >= cantidad) en vez de leer-chequear-escribir:
        // dos confirmaciones de pago concurrentes para el mismo producto (p.ej. reconfirmar
        // /api/pedidos/mp/exito con recarga o doble navegación) podían pasar ambas el chequeo de
        // stock de arriba antes de que cualquiera guardara, dejando stock negativo. El motor de
        // base de datos serializa la fila; si esta llamada no logra actualizar ninguna fila es
        // porque ya no había stock suficiente.
        int filasActualizadas = productoRepository.reducirStockAtomico(productoId, cantidad);
        if (filasActualizadas == 0) {
            throw new SolicitudInvalidaException("Stock insuficiente para el producto: " + producto.getNombre()
                    + ". Stock disponible: " + producto.getStock() + ", solicitado: " + cantidad);
        }

        int stockAnterior = producto.getStock();

        // clearAutomatically=true en reducirStockAtomico limpia el contexto de persistencia, así
        // que esta relectura trae el stock real post-UPDATE (no el valor cacheado de "producto").
        Producto actualizado = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + productoId));

        // Si el stock llega a 0, cambiar estado a "No disponible"
        if (actualizado.getStock() == 0 && !"No disponible".equals(actualizado.getEstadoProducto())) {
            actualizado.setEstadoProducto("No disponible");
            productoRepository.save(actualizado);
        }

        avisarStockBajoSiCorresponde(actualizado, stockAnterior, actualizado.getStock());

        System.out.println("✅ Stock reducido. Producto: " + actualizado.getNombre()
                + ", Nuevo stock: " + actualizado.getStock());
    }

    /**
     * Reducir stock de una talla específica (para ropa - categoría 3)
     */
    @Transactional
    public void reducirStockPorTalla(Long productoId, String talla, Integer cantidad) {
        // Buscar la talla específica
        TallaStock tallaStock = tallaStockRepository.findByProductoIdAndTalla(productoId, talla)
                .orElseThrow(() -> new RecursoNoEncontradoException("Talla " + talla + " no encontrada para el producto ID: " + productoId));

        // Verificar stock suficiente
        if (tallaStock.getStock() < cantidad) {
            throw new SolicitudInvalidaException("Stock insuficiente para la talla " + talla
                    + ". Stock disponible: " + tallaStock.getStock() + ", solicitado: " + cantidad);
        }

        // Reducir stock de la talla
        tallaStock.setStock(tallaStock.getStock() - cantidad);
        tallaStockRepository.save(tallaStock);

        // Actualizar stock general del producto
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));

        int stockAnterior = producto.getStock() != null ? producto.getStock() : 0;

        // Calcular nuevo stock general sumando todas las tallas
        Integer stockGeneral = tallaStockRepository.sumStockByProductoId(productoId);
        producto.setStock(stockGeneral);

        // Si stock general es 0, cambiar estado
        if (stockGeneral == 0) {
            producto.setEstadoProducto("No disponible");
        }

        productoRepository.save(producto);
        avisarStockBajoSiCorresponde(producto, stockAnterior, stockGeneral);

        System.out.println("✅ Stock reducido por talla. Producto: " + producto.getNombre()
                + ", Talla: " + talla + ", Nuevo stock: " + tallaStock.getStock()
                + ", Stock general: " + stockGeneral);
    }

    /**
     * Verificar si un producto tiene suficiente stock
     */
    public boolean verificarStockSuficiente(Long productoId, Integer cantidad) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));

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
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
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

    // Avisa al vendedor (notificación in-app + correo) solo al CRUZAR el umbral hacia abajo
    // (stockAnterior >= umbral && stockNuevo < umbral) — evita mandar un aviso por cada venta
    // mientras el producto se mantenga por debajo del umbral.
    private void avisarStockBajoSiCorresponde(Producto producto, int stockAnterior, int stockNuevo) {
        if (stockAnterior < UMBRAL_STOCK_BAJO || stockNuevo >= UMBRAL_STOCK_BAJO) return;

        User vendedor = producto.getVendedor();
        if (vendedor == null) return;

        String mensaje = stockNuevo == 0
                ? "Tu producto \"" + producto.getNombre() + "\" se quedó sin stock."
                : "Tu producto \"" + producto.getNombre() + "\" tiene poco stock: quedan " + stockNuevo + " unidades.";

        notificacionService.crear(vendedor, mensaje, "STOCK_BAJO", null, producto.getId());
        if (vendedor.getEmail() != null && !vendedor.getEmail().isBlank()) {
            emailService.enviarCorreo(vendedor.getEmail(), "Stock bajo: " + producto.getNombre() + " - MarketS", mensaje);
        }
    }
}