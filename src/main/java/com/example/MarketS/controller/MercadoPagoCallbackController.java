package com.example.MarketS.controller;

import com.example.MarketS.model.Pedido;
import com.example.MarketS.repository.PedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/mercadopago")
public class MercadoPagoCallbackController {

    @Autowired
    private PedidoRepository pedidoRepository;

    /**
     * Callback de éxito desde MercadoPago
     */
    @GetMapping("/exito")
    public String exito(@RequestParam Map<String, String> params,
                        @RequestParam(required = false) Long pedidoId,
                        Model model) {

        System.out.println("\n🎉🎉🎉 CALLBACK EXITO MERCADOPAGO 🎉🎉🎉");
        System.out.println("Parámetros recibidos: " + params);

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

            // Redirigir a la confirmación
            return "redirect:/carrito?step=3&pedidoId=" + idPedido + "&status=" + params.get("status");

        } catch (Exception e) {
            System.err.println("❌ Error en callback de éxito: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error procesando el pago: " + e.getMessage());
            return "pago/error";
        }
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
            // En algunos casos el ID viene aquí
            System.out.println("collection_id encontrado: " + params.get("collection_id"));
        }

        return null;
    }

    /**
     * Método helper para procesar el estado del pago
     */
    private void procesarEstadoPago(Map<String, String> params, Pedido pedido) {
        String status = params.get("status");
        String paymentId = params.get("payment_id");

        System.out.println("📋 Detalles del pago:");
        System.out.println("   - Pedido ID: " + pedido.getId());
        System.out.println("   - Status: " + status);
        System.out.println("   - Payment ID: " + paymentId);

        // Actualizar estado del pedido según el resultado
        if ("approved".equals(status)) {
            pedido.setEstado("PAGADO");
            System.out.println("✅ Pago APROBADO - Marcando como PAGADO");
        } else if ("pending".equals(status)) {
            pedido.setEstado("PENDIENTE_PAGO");
            System.out.println("⚠️ Pago PENDIENTE");
        } else if ("rejected".equals(status)) {
            pedido.setEstado("RECHAZADO");
            System.out.println("❌ Pago RECHAZADO");
        } else {
            pedido.setEstado("PROCESANDO");
            System.out.println("🔄 Estado desconocido: " + status);
        }

        pedidoRepository.save(pedido);
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

            model.addAttribute("mensaje", "Tu pago está pendiente de confirmación.");
            model.addAttribute("pedidoId", idPedido);

            return "pago/pendiente";

        } catch (Exception e) {
            System.err.println("Error en callback pendiente: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "pago/error";
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

            model.addAttribute("mensaje", "El pago no se pudo completar. Por favor, intenta nuevamente.");
            model.addAttribute("pedidoId", idPedido);

            return "pago/fallo";

        } catch (Exception e) {
            System.err.println("Error en callback fallo: " + e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "pago/error";
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

        // Aquí procesarías las notificaciones en tiempo real
        // Por ahora solo logueamos
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