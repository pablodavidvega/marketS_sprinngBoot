package com.example.MarketS.controller;
import com.example.MarketS.service.MercadoPagoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import com.example.MarketS.model.Pedido;
import com.example.MarketS.model.User;
import com.example.MarketS.repository.PedidoRepository;
import com.example.MarketS.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;

@Controller
@RequestMapping("/carrito")
public class CarritoController {

    @Autowired
    private final UserService userService;
    @Autowired
    private MercadoPagoService mercadoPagoService;
    // Inyectar PedidoRepository
    @Autowired
    private PedidoRepository pedidoRepository;

    public CarritoController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String mostrarCarrito(HttpSession session, Model model,
                                 @RequestParam(required = false) String step,
                                 @RequestParam(required = false) Long pedidoId,
                                 @RequestParam(required = false) String status,
                                 HttpServletRequest request) {

        System.out.println("\n=== 🛒 CARRITO CONTROLLER: mostrarCarrito ===");
        System.out.println("Step: " + step + ", PedidoId: " + pedidoId);

        try {
            Map<String, Map<String, Object>> carrito = obtenerCarrito(session);

            // Calcular totales
            double subtotal = 0;
            for (Map<String, Object> item : carrito.values()) {
                double precio = (double) item.get("precio");
                int cantidad = (int) item.get("cantidad");
                subtotal += precio * cantidad;
            }

            double descuentoAplicado = 0;
            double totalFinal = subtotal - descuentoAplicado;

            model.addAttribute("carrito", carrito.values());
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("descuentoAplicado", descuentoAplicado);
            model.addAttribute("totalFinal", totalFinal);

            // Datos de usuario
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String email = auth.getName();

            if (email != null && !email.equals("anonymousUser")) {
                User user = userService.buscarPorEmail(email).orElse(null);
                if (user != null) {
                    model.addAttribute("nombreUsuario", user.getUsername());
                    model.addAttribute("rolUsuario", user.getRol().getRolname());
                }
            }

            // Manejar steps
            if (step != null) {
                model.addAttribute("currentStep", step);

                if ("2".equals(step)) {
                    System.out.println("🔍 Mostrando paso 2 (Pago)");

                    // 1. Intentar obtener datos de flash attributes
                    Map<String, ?> flashAttributes = RequestContextUtils.getInputFlashMap(request);
                    if (flashAttributes != null) {
                        System.out.println("📦 Flash attributes encontrados:");
                        flashAttributes.forEach((key, value) -> {
                            System.out.println("   - " + key + ": " + value);
                            model.addAttribute(key, value);
                        });
                    }

                    // 2. Intentar cargar pedido desde BD si tenemos ID
                    if (pedidoId != null) {
                        System.out.println("🔍 Buscando pedido en BD: " + pedidoId);
                        try {
                            Optional<Pedido> pedidoOpt = pedidoRepository.findById(pedidoId);
                            if (pedidoOpt.isPresent()) {
                                Pedido pedido = pedidoOpt.get();
                                model.addAttribute("pedido", pedido);
                                System.out.println("✅ Pedido cargado: #" + pedido.getId());

                                // Si no hay paymentUrl en flash, intentar generar uno
                                if (!model.containsAttribute("paymentUrl")) {
                                    try {
                                        System.out.println("🔄 Generando paymentUrl para pedido existente...");
                                        Map<String, Object> resultadoMP = mercadoPagoService.crearPreferencia(
                                                pedido.getId(),
                                                pedido.getTotal(),
                                                "Compra MarketS - Pedido #" + pedido.getId(),
                                                pedido.getUsuario().getEmail()
                                        );

                                        if (Boolean.TRUE.equals(resultadoMP.get("success"))) {
                                            model.addAttribute("paymentUrl", resultadoMP.get("paymentUrl"));
                                            System.out.println("✅ Payment URL generada: " + resultadoMP.get("paymentUrl"));
                                        }
                                    } catch (Exception e) {
                                        System.err.println("⚠️ Error generando paymentUrl: " + e.getMessage());
                                    }
                                }
                            } else {
                                System.out.println("⚠️ Pedido no encontrado en BD: " + pedidoId);
                                model.addAttribute("error", "Pedido no encontrado");
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Error cargando pedido: " + e.getMessage());
                            model.addAttribute("error", "Error cargando pedido: " + e.getMessage());
                        }
                    }

                } else if ("3".equals(step)) {
                    model.addAttribute("statusPago", status);
                    model.addAttribute("pedidoId", pedidoId);
                }
            } else {
                model.addAttribute("currentStep", "1");
            }

            System.out.println("✅ Retornando vista carrito.html");
            return "carrito-simple";

        } catch (Exception e) {
            System.err.println("❌ ERROR CRÍTICO en mostrarCarrito:");
            System.err.println("Mensaje: " + e.getMessage());
            System.err.println("Stack trace:");
            e.printStackTrace();

            // Redirigir a página de error o carrito vacío
            return "redirect:/carrito?error=" + e.getMessage();
        }
    }

    @GetMapping("/agregar")
    public String agregarAlCarrito(@RequestParam String id,
                                   @RequestParam String nombre,
                                   @RequestParam double precio,
                                   @RequestParam int cantidad,
                                   HttpSession session) {

        Map<String, Map<String, Object>> carrito = obtenerCarrito(session);

        if (carrito.containsKey(id)) {
            int cantidadExistente = (int) carrito.get(id).get("cantidad");
            carrito.get(id).put("cantidad", cantidadExistente + cantidad);
        } else {
            Map<String, Object> producto = new HashMap<>();
            producto.put("id", id);
            producto.put("nombre", nombre);
            producto.put("precio", precio);
            producto.put("cantidad", cantidad);
            carrito.put(id, producto);
        }

        session.setAttribute("carrito", carrito);
        return "redirect:/carrito";
    }


    @PostMapping("/eliminar/{id}")
    public String eliminarDelCarrito(@PathVariable String id, HttpSession session) {
        Map<String, Map<String, Object>> carrito = obtenerCarrito(session);
        carrito.remove(id);
        session.setAttribute("carrito", carrito);
        return "redirect:/carrito";
    }

    private Map<String, Map<String, Object>> obtenerCarrito(HttpSession session) {
        Map<String, Map<String, Object>> carrito =
                (Map<String, Map<String, Object>>) session.getAttribute("carrito");

        if (carrito == null) {
            carrito = new LinkedHashMap<>();
            session.setAttribute("carrito", carrito);
        }

        return carrito;
    }

    @PostMapping("/incrementar/{id}")
    public String incrementarCantidad(@PathVariable String id, HttpSession session) {
        Map<String, Map<String, Object>> carrito = obtenerCarrito(session);
        if (carrito.containsKey(id)) {
            int cantidadActual = (int) carrito.get(id).get("cantidad");
            carrito.get(id).put("cantidad", cantidadActual + 1);
        }
        session.setAttribute("carrito", carrito);
        return "redirect:/carrito";
    }

    @PostMapping("/decrementar/{id}")
    public String decrementarCantidad(@PathVariable String id, HttpSession session) {
        Map<String, Map<String, Object>> carrito = obtenerCarrito(session);
        if (carrito.containsKey(id)) {
            int cantidadActual = (int) carrito.get(id).get("cantidad");
            if (cantidadActual > 1) {
                carrito.get(id).put("cantidad", cantidadActual - 1);
            } else {
                carrito.remove(id);
            }
        }
        session.setAttribute("carrito", carrito);
        return "redirect:/carrito";
    }
}