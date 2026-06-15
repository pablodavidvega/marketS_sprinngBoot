package com.example.MarketS.controller;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.DetallePedido;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/mercadopago")
public class MercadoPagoCallbackController {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoService productoService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Callback de éxito desde MercadoPago
     */
    @GetMapping("/exito")
    public String exito(@RequestParam Map<String, String> params,
                        @RequestParam(required = false) Long pedidoId,
                        Model model) {
        System.out.println("\n🎉🎉🎉 CALLBACK EXITO MERCADOPAGO 🎉🎉🎉");
        System.out.println("Parámetros recibidos: " + params);

        // ========== DEBUG DEL ESTADO ==========
        System.out.println("\n🎉🎉🎉 CALLBACK EXITO MERCADOPAGO 🎉🎉🎉");
        System.out.println("Parámetros recibidos: " + params);

        // ========== DEBUG DEL ESTADO ==========
        System.out.println("\n🔍 DEBUG DEL ESTADO:");
        System.out.println("1. status: " + params.get("status"));
        System.out.println("2. collection_status: " + params.get("collection_status"));
        System.out.println("3. payment_status: " + params.get("payment_status"));
        System.out.println("4. external_reference: " + params.get("external_reference"));
        System.out.println("5. preference_id: " + params.get("preference_id"));
        System.out.println("6. payment_id: " + params.get("payment_id"));
        System.out.println("======================================");

        try {
            // Obtener ID del pedido
            Long idPedido = obtenerIdPedido(params, pedidoId);

            if (idPedido == null) {
                System.err.println("❌ No se pudo obtener el ID del pedido");
                model.addAttribute("error", "No se pudo identificar el pedido");
                return "pago/error";
            }

            // Buscar el pedido
            Pedido pedido = pedidoRepository.findById(idPedido)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + idPedido));

            procesarEstadoPago(params, pedido);

            // ========== NUEVO: Reducir stock solo si el pago fue aprobado ==========
            if ("approved".equals(params.get("status"))) {
                try {
                    reducirStockDeProductos(pedido);
                } catch (Exception e) {
                    // Registrar error pero continuar con el flujo (no interrumpir el proceso de pago)
                    System.err.println("⚠️ Error al reducir stock (continuando con el pago): " + e.getMessage());
                }
            }

            // Redirigir a la confirmación
            return "redirect:/carrito?step=3&pedidoId=" + idPedido + "&status=" + params.get("status");

        } catch (Exception e) {
            System.err.println("❌ Error en callback de éxito: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error procesando el pago: " + e.getMessage());
            return "pago/error";
        }
    }

    @Controller
    @RequestMapping("/pedido")
    public class PedidoController {

        @Autowired
        private PedidoRepository pedidoRepository;

        @Autowired
        private ProductoService productoService;

        /**
         * Confirmación manual de pago (para cuando el usuario ya pagó en MercadoPago)
         */
        @PostMapping("/confirmar-pago/{pedidoId}")
        public String confirmarPagoManual(@PathVariable Long pedidoId,
                                          @RequestParam(required = false) String origen,
                                          RedirectAttributes redirectAttributes) {

            System.out.println("\n🔄 CONFIRMACIÓN MANUAL DE PAGO");
            System.out.println("Pedido ID: " + pedidoId);
            System.out.println("Origen: " + (origen != null ? origen : "desconocido"));

            try {
                // Buscar el pedido
                Pedido pedido = pedidoRepository.findById(pedidoId)
                        .orElseThrow(() -> new RuntimeException("Pedido no encontrado: " + pedidoId));

                System.out.println("📋 Estado actual del pedido: " + pedido.getEstado());

                // Verificar si ya está pagado
                if ("PAGADO".equals(pedido.getEstado())) {
                    System.out.println("⚠️ El pedido ya está marcado como PAGADO");
                    redirectAttributes.addFlashAttribute("info", "Este pedido ya está pagado.");
                    return "redirect:/carrito?step=3&pedidoId=" + pedidoId;
                }

                // Marcar como pagado
                pedido.setEstado("PAGADO");
                pedidoRepository.save(pedido);
                System.out.println("✅ Pedido marcado como PAGADO");

                // Reducir stock de productos
                reducirStockDeProductos(pedido);

                // Registrar la acción
                System.out.println("🎉 Confirmación manual completada para pedido #" + pedidoId);

                // Redirigir a la confirmación (paso 3)
                redirectAttributes.addFlashAttribute("success", "¡Pago confirmado exitosamente!");
                return "redirect:/carrito?step=3&pedidoId=" + pedidoId;

            } catch (Exception e) {
                System.err.println("❌ Error en confirmación manual: " + e.getMessage());
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error",
                        "Error al confirmar el pago: " + e.getMessage());
                return "redirect:/carrito?step=2&pedidoId=" + pedidoId;
            }
        }

        /**
         * Método para reducir stock (reutilizable)
         */
        private void reducirStockDeProductos(Pedido pedido) {
            if (pedido == null) {
                System.err.println("❌ Pedido es null, no se puede reducir stock");
                return;
            }

            System.out.println("\n🔄 REDUCIENDO STOCK DE PEDIDO #" + pedido.getId());

            if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
                System.out.println("⚠️ El pedido no tiene detalles");
                return;
            }

            int productosActualizados = 0;
            for (DetallePedido detalle : pedido.getDetalles()) {
                try {
                    Long productoId = detalle.getProducto().getId();
                    Integer cantidad = detalle.getCantidad();

                    System.out.println("📦 Reduciendo " + cantidad + " unidades del producto ID: " + productoId);

                    // Reducir stock
                    productoService.reducirStock(productoId, cantidad);
                    productosActualizados++;

                    System.out.println("✅ Stock reducido exitosamente");

                } catch (Exception e) {
                    System.err.println("❌ Error al reducir stock del producto: " + e.getMessage());
                    // Continuar con los demás productos
                }
            }

            System.out.println("✅ Stock reducido para " + productosActualizados + " productos del pedido #" + pedido.getId());
        }

        /**
         * Endpoint alternativo (GET) para confirmación rápida
         */
        @GetMapping("/confirmar/{pedidoId}")
        public String confirmarPagoRapido(@PathVariable Long pedidoId,
                                          RedirectAttributes redirectAttributes) {
            return confirmarPagoManual(pedidoId, "rapido", redirectAttributes);
        }
    }

    /**
     * NUEVO MÉTODO: Reducir stock de los productos del pedido
     * Se llama automáticamente cuando un pago es aprobado
     */
    private void reducirStockDeProductos(Pedido pedido) {
        if (pedido == null) {
            System.err.println("❌ Pedido es null, no se puede reducir stock");
            return;
        }

        System.out.println("\n🔄 REDUCIENDO STOCK DE PEDIDO #" + pedido.getId());

        // Verificar si ya se redujo el stock (para evitar duplicados)
//        if (pedido.getStockReducido() != null && pedido.getStockReducido()) {
//            System.out.println("⚠️ Stock ya reducido anteriormente para este pedido");
//            return;
//        }

        if (pedido.getDetalles() == null || pedido.getDetalles().isEmpty()) {
            System.out.println("⚠️ El pedido no tiene detalles");
            return;
        }

        for (DetallePedido detalle : pedido.getDetalles()) {
            try {
                Long productoId = detalle.getProducto().getId();
                Integer cantidad = detalle.getCantidad();

                System.out.println("📦 Reduciendo " + cantidad + " unidades del producto ID: " + productoId);

                // LLAMADA AL SERVICIO
                productoService.reducirStock(productoId, cantidad);

                System.out.println("✅ Stock reducido exitosamente");

            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }

        // Marcar que el stock ya fue reducido
        // pedido.setStockReducido(true); // Necesitarías agregar este campo a la entidad Pedido
        // pedidoRepository.save(pedido);

        System.out.println("✅ Stock reducido completamente para pedido #" + pedido.getId());
    }

    private Long obtenerIdPedido(Map<String, String> params, Long pedidoIdParam) {
        if (pedidoIdParam != null) {
            return pedidoIdParam;
        }

        if (params.containsKey("external_reference")) {
            try {
                return Long.parseLong(params.get("external_reference"));
            } catch (NumberFormatException e) {
                System.err.println("❌ Error parseando external_reference: " + params.get("external_reference"));
            }
        }

        // Intentar extraer de la URL o otros parámetros
        if (params.containsKey("collection_id")) {
            System.out.println("collection_id encontrado: " + params.get("collection_id"));
        }

        return null;
    }

    /**
     * Método helper para procesar el estado del pago
     */
    private void procesarEstadoPago(Map<String, String> params, Pedido pedido) {
        String status = params.get("status");
        String collectionStatus = params.get("collection_status"); // NUEVO
        String paymentId = params.get("payment_id");

        System.out.println("📋 Detalles del pago:");
        System.out.println("   - Pedido ID: " + pedido.getId());
        System.out.println("   - Status: " + status);
        System.out.println("   - Collection Status: " + collectionStatus); // NUEVO
        System.out.println("   - Payment ID: " + paymentId);

        // DECISIÓN: Para Sandbox, considerar "approved" o "pending"
        String estadoFinal = "PROCESANDO";

        if ("approved".equals(status) || "approved".equals(collectionStatus)) {
            estadoFinal = "PAGADO";
            System.out.println("✅ Pago APROBADO - Marcando como PAGADO");
        } else if ("pending".equals(status) || "pending".equals(collectionStatus)) {
            estadoFinal = "PENDIENTE_PAGO";
            System.out.println("⚠️ Pago PENDIENTE");

            // PARA SANDBOX: Auto-aprobar pagos pendientes (solo testing!)
            if (esAmbienteSandbox()) {
                System.out.println("🔄 SANDBOX: Auto-aprobando pago pendiente para testing");
                estadoFinal = "PAGADO";
            }
        } else if ("rejected".equals(status) || "rejected".equals(collectionStatus)) {
            estadoFinal = "RECHAZADO";
            System.out.println("❌ Pago RECHAZADO");
        }

        pedido.setEstado(estadoFinal);
        pedidoRepository.save(pedido);

        // ========== NUEVO: Reducir stock si el estado es PAGADO ==========
        if ("PAGADO".equals(estadoFinal)) {
            reducirStockDeProductos(pedido);
        }
    }

    // Método auxiliar para detectar si estamos en Sandbox
    private boolean esAmbienteSandbox() {
        // Verifica si el token de MercadoPago es de Sandbox (empieza con TEST-)
        // O puedes crear una variable de entorno
        return System.getenv("MP_ENV") != null &&
                System.getenv("MP_ENV").equals("sandbox");
    }

    /**
     * Callback de pendiente
     */
    @GetMapping("/pendiente")
    public String pendiente(@RequestParam Map<String, String> params,
                            @RequestParam(required = false) Long pedidoId,
                            Model model) {

        System.out.println("\n⏳ CALLBACK PENDIENTE MERCADOPAGO");

        try {
            Long idPedido = obtenerIdPedido(params, pedidoId);

            if (idPedido != null) {
                Pedido pedido = pedidoRepository.findById(idPedido).orElse(null);
                if (pedido != null) {
                    pedido.setEstado("PENDIENTE_PAGO");
                    pedidoRepository.save(pedido);
                    System.out.println("📝 Pedido #" + idPedido + " marcado como PENDIENTE_PAGO");
                }
            }

            String redirect = frontendUrl + "/pagos/pendiente" + (idPedido != null ? "?pedido_id=" + idPedido : "");
            return "redirect:" + redirect;

        } catch (Exception e) {
            System.err.println("Error en callback pendiente: " + e.getMessage());
            return "redirect:" + frontendUrl + "/pagos/pendiente";
        }
    }

    /**
     * Callback de fallo
     */
    @GetMapping("/fallo")
    public String fallo(@RequestParam Map<String, String> params,
                        @RequestParam(required = false) Long pedidoId,
                        Model model) {

        System.out.println("\n❌ CALLBACK FALLO MERCADOPAGO");

        try {
            Long idPedido = obtenerIdPedido(params, pedidoId);

            if (idPedido != null) {
                Pedido pedido = pedidoRepository.findById(idPedido).orElse(null);
                if (pedido != null) {
                    pedido.setEstado("RECHAZADO");
                    pedidoRepository.save(pedido);
                    System.out.println("📝 Pedido #" + idPedido + " marcado como RECHAZADO");
                }
            }

            String redirect = frontendUrl + "/pagos/fallo" + (idPedido != null ? "?pedido_id=" + idPedido : "");
            return "redirect:" + redirect;

        } catch (Exception e) {
            System.err.println("Error en callback fallo: " + e.getMessage());
            return "redirect:" + frontendUrl + "/pagos/fallo";
        }
    }

    /**
     * Webhook para notificaciones de MercadoPago
     */
    @PostMapping("/webhook")
    @ResponseBody
    public String webhook(@RequestParam Map<String, String> params) {
        System.out.println("\n📢 WEBHOOK MERCADOPAGO RECIBIDO");
        System.out.println("Params: " + params);

        // OPCIONAL: También podrías procesar el webhook para reducir stock
        // pero el callback /exito ya lo hace
        return "OK";
    }

    /**
     * Nueva ruta para confirmación
     */
    @GetMapping("/confirmacion/{pedidoId}")
    public String confirmacion(@PathVariable Long pedidoId,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String origen,
                               Model model) {

        try {
            Pedido pedido = pedidoRepository.findById(pedidoId)
                    .orElseThrow(() -> new RuntimeException("Pedido no encontrado"));

            model.addAttribute("pedido", pedido);
            model.addAttribute("status", status);
            model.addAttribute("origen", origen != null ? origen : "mercadopago");

            return "confirmacion";

        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar confirmación: " + e.getMessage());
            return "pago/error";
        }
    }
}